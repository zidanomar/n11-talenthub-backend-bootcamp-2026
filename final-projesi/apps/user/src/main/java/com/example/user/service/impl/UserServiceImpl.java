package com.example.user.service.impl;

import com.example.user.config.KeycloakProperties;
import com.example.user.dto.AddressResponse;
import com.example.user.dto.ChangePasswordRequest;
import com.example.user.dto.CreateAddressRequest;
import com.example.user.dto.SigninRequest;
import com.example.user.dto.SignupRequest;
import com.example.user.dto.SignupResponse;
import com.example.user.dto.UpdateAddressRequest;
import com.example.user.dto.UserResponse;
import com.example.user.entity.Address;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.exception.KeycloakException;
import com.example.user.repository.AddressRepository;
import com.example.user.repository.UserRepository;
import com.example.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(1);

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final KeycloakProperties keycloak;
    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public SignupResponse signup(SignupRequest request) {
        String adminToken = getAdminToken();

        Map<String, Object> keycloakUser = Map.of(
                "username", request.username(),
                "email", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false
                ))
        );

        var response = webClient.post()
                .uri(keycloak.getServerUrl() + "/admin/realms/" + keycloak.getRealm() + "/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(keycloakUser)
                .retrieve()
                .toBodilessEntity()
                .onErrorMap(e -> {
                    String msg = e.getMessage() != null && e.getMessage().contains("409")
                            ? "Username or email already exists" : "Signup failed: " + e.getMessage();
                    int status = e.getMessage() != null && e.getMessage().contains("409") ? 409 : 400;
                    return new KeycloakException(msg, status);
                })
                .block();

        String location = response.getHeaders().getFirst("Location");
        if (location == null) throw new KeycloakException("Keycloak did not return user location", 500);
        String keycloakId = location.substring(location.lastIndexOf('/') + 1);

        var user = User.builder()
                .id(keycloakId)
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role() != null ? request.role() : Role.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .build();

        transactionTemplate.executeWithoutResult(status -> userRepository.save(user));
        log.info("User created: id={} username={}", keycloakId, request.username());
        return SignupResponse.from(user);
    }

    @Override
    public Map<String, Object> signin(SigninRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloak.getClient().getId());
        form.add("client_secret", keycloak.getClient().getSecret());
        form.add("username", request.username());
        form.add("password", request.password());

        Map<String, Object> keycloakResponse = webClient.post()
                .uri(keycloak.getServerUrl() + "/realms/" + keycloak.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(e -> new KeycloakException("Invalid credentials", 401))
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block();

        String refreshToken = (String) keycloakResponse.get("refresh_token");
        redisTemplate.opsForValue().set("refresh:" + request.username(), refreshToken, REFRESH_TOKEN_TTL);

        return buildTokenResponse(keycloakResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(String userId) {
        return buildUserResponse(findUser(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String userId) {
        return findUser(userId).getAddresses().stream().map(AddressResponse::from).toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(String userId, CreateAddressRequest request) {
        var user = findUser(userId);
        Address a = applyToAddress(new Address(), user, request.label(), request.phone(),
                request.identityNumber(), request.addressLine(), request.city(),
                request.country(), request.zipCode());
        log.info("Address created for user={}", userId);
        return AddressResponse.from(addressRepository.save(a));
    }

    @Override
    @Transactional
    public AddressResponse updateAddressById(String userId, Long addressId, CreateAddressRequest request) {
        Address a = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        applyToAddress(a, a.getUser(), request.label(), request.phone(),
                request.identityNumber(), request.addressLine(), request.city(),
                request.country(), request.zipCode());
        log.info("Address {} updated for user={}", addressId, userId);
        return AddressResponse.from(addressRepository.save(a));
    }

    @Override
    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        Address a = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        User user = a.getUser();
        addressRepository.delete(a);
        if (user.getCurrentAddress() != null && addressId.equals(user.getCurrentAddress().getId())) {
            user.setCurrentAddress(null);
            userRepository.save(user);
        }
        log.info("Address {} deleted for user={}", addressId, userId);
    }

    @Override
    @Transactional
    public UserResponse setCurrentAddress(String userId, Long addressId) {
        var user = findUser(userId);
        Address a = addressRepository.findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        user.setCurrentAddress(a);
        userRepository.save(user);
        log.info("User {} current address set to {}", userId, addressId);
        return buildUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateAddress(String userId, UpdateAddressRequest request) {
        var user = findUser(userId);
        Address address = user.getCurrentAddress() != null
                ? user.getCurrentAddress()
                : new Address();
        applyToAddress(address, user, request.label(), request.phone(),
                request.identityNumber(), request.addressLine(), request.city(),
                request.country(), request.zipCode());
        addressRepository.save(address);
        user.setCurrentAddress(address);
        userRepository.save(user);
        log.info("User {} current address upserted id={}", userId, address.getId());
        return buildUserResponse(user);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        var user = findUser(userId);

        if (request.oldPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must differ from old password");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloak.getClient().getId());
        form.add("client_secret", keycloak.getClient().getSecret());
        form.add("username", user.getUsername());
        form.add("password", request.oldPassword());

        try {
            webClient.post()
                    .uri(keycloak.getServerUrl() + "/realms/" + keycloak.getRealm()
                            + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
        }

        String adminToken = getAdminToken();
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", request.newPassword(),
                "temporary", false
        );

        try {
            webClient.put()
                    .uri(keycloak.getServerUrl() + "/admin/realms/" + keycloak.getRealm()
                            + "/users/" + userId + "/reset-password")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credential)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new KeycloakException("Password update failed: " + e.getMessage(), 500);
        }

        redisTemplate.delete("refresh:" + user.getUsername());
        log.info("Password changed for user {}", userId);
    }

    @Override
    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloak.getClient().getId());
        form.add("client_secret", keycloak.getClient().getSecret());
        form.add("refresh_token", refreshToken);

        @SuppressWarnings("unchecked")
        Map<String, Object> keycloakResponse = webClient.post()
                .uri(keycloak.getServerUrl() + "/realms/" + keycloak.getRealm()
                        + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(e -> new KeycloakException("Invalid or expired refresh token", 401))
                .map(m -> (Map<String, Object>) m)
                .block();

        String username = extractPreferredUsername((String) keycloakResponse.get("access_token"));
        String newRefreshToken = (String) keycloakResponse.get("refresh_token");
        if (username != null && newRefreshToken != null) {
            redisTemplate.opsForValue().set("refresh:" + username, newRefreshToken, REFRESH_TOKEN_TTL);
        }

        return buildTokenResponse(keycloakResponse);
    }

    private Map<String, Object> buildTokenResponse(Map<String, Object> keycloakResponse) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("access_token", keycloakResponse.get("access_token"));
        body.put("token_type", keycloakResponse.get("token_type"));
        body.put("expires_in", keycloakResponse.get("expires_in"));
        body.put("refresh_token", keycloakResponse.get("refresh_token"));
        body.put("refresh_expires_in", keycloakResponse.get("refresh_expires_in"));
        return body;
    }

    private String extractPreferredUsername(String accessToken) {
        if (accessToken == null) return null;
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) return null;
            byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);
            return (String) claims.get("preferred_username");
        } catch (Exception e) {
            return null;
        }
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Address applyToAddress(Address a, User user, String label,
                                   String phone, String identityNumber, String addressLine,
                                   String city, String country, String zipCode) {
        a.setUser(user);
        a.setLabel(label);
        a.setPhone(phone);
        a.setIdentityNumber(identityNumber);
        a.setAddressLine(addressLine);
        a.setCity(city);
        a.setCountry(country);
        a.setZipCode(zipCode);
        return a;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private UserResponse buildUserResponse(User user) {
        List<AddressResponse> addresses = user.getAddresses().stream()
                .map(AddressResponse::from).toList();
        return UserResponse.from(user, user.getCurrentAddress(), addresses);
    }

    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", keycloak.getAdmin().getUsername());
        form.add("password", keycloak.getAdmin().getPassword());

        Map<?, ?> result = webClient.post()
                .uri(keycloak.getServerUrl() + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorMap(e -> new KeycloakException("Admin auth failed", 500))
                .block();

        return (String) result.get("access_token");
    }
}
