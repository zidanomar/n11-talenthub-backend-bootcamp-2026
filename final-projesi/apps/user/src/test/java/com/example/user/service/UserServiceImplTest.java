package com.example.user.service;

import com.example.user.config.KeycloakProperties;
import com.example.user.dto.UpdateAddressRequest;
import com.example.user.entity.Address;
import com.example.user.entity.User;
import com.example.user.repository.AddressRepository;
import com.example.user.repository.UserRepository;
import com.example.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private KeycloakProperties keycloak;
    @Mock private WebClient webClient;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser(String id) {
        return User.builder()
                .id(id)
                .username("testuser")
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .createdAt(LocalDateTime.now())
                .addresses(new ArrayList<>())
                .build();
    }

    @Test
    void getById_existingUser_returnsUserResponse() {
        var user = sampleUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));

        var result = userService.getById("user1");

        assertThat(result.id()).isEqualTo("user1");
        assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    void getById_unknownUser_throws404() {
        when(userRepository.findById("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById("nobody"))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 404);
    }

    @Test
    void getAddresses_returnsUserAddresses() {
        var user = sampleUser("user1");
        var address = new Address();
        address.setLabel("Home");
        address.setAddressLine("Main St 1");
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setZipCode("34000");
        user.getAddresses().add(address);
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));

        var result = userService.getAddresses("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).label()).isEqualTo("Home");
    }

    @Test
    void updateAddress_setsCurrentAddressAndSaves() {
        var user = sampleUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateAddressRequest(null, "+905001234567", "12345678901",
                "Test St 1", "Istanbul", "Turkey", "34000");
        var result = userService.updateAddress("user1", request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("user1");
        verify(addressRepository).save(any(Address.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateAddress_updatesExistingCurrentAddress() {
        var user = sampleUser("user1");
        var existing = new Address();
        existing.setId(1L);
        existing.setUser(user);
        user.setCurrentAddress(existing);
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateAddressRequest(null, "+905001234567", "12345678901",
                "New St 2", "Ankara", "Turkey", "06000");
        userService.updateAddress("user1", request);

        var captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getCity()).isEqualTo("Ankara");
    }

    @Test
    void deleteAddress_notFound_throws404() {
        when(addressRepository.findByIdAndUser_Id(99L, "user1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAddress("user1", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 404);
    }
}
