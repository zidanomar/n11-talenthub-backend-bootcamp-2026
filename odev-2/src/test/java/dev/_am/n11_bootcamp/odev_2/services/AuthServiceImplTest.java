package dev._am.n11_bootcamp.odev_2.services;

import dev._am.n11_bootcamp.odev_2.dto.AuthResponse;
import dev._am.n11_bootcamp.odev_2.dto.SignupRequest;
import dev._am.n11_bootcamp.odev_2.repositories.UserRepository;
import dev._am.n11_bootcamp.odev_2.repositories.impl.UserRepositoryImpl;
import dev._am.n11_bootcamp.odev_2.services.impl.AuthServiceImpl;
import dev._am.n11_bootcamp.odev_2.services.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceImplTest {

    private AuthService authService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    private static final String SECRET = "c2VjcmV0LWtleS1mb3ItdGVzdC1wdXJwb3Nlcy1vbmx5LTMyY2hhcg==";

    @BeforeEach
    void setUp() {
        userRepository = new UserRepositoryImpl();
        passwordEncoder = new BCryptPasswordEncoder(4);
        JwtService jwtService = new JwtServiceImpl(SECRET, 1L, 3L);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void signup_shouldSaveUserWithHashedPassword() {
        authService.signup(new SignupRequest("sauron", "mordor123"));

        var saved = userRepository.findByUsername("sauron");
        assertNotNull(saved);
        assertNotEquals("mordor123", saved.getPassword());
        assertTrue(passwordEncoder.matches("mordor123", saved.getPassword()));
    }

    @Test
    void signup_shouldReturnValidJwtTokens() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 1L, 3L);
        AuthResponse response = authService.signup(new SignupRequest("sauron", "mordor123"));

        assertTrue(jwtService.isTokenValid(response.getAccessToken()));
        assertTrue(jwtService.isTokenValid(response.getRefreshToken()));
    }

    @Test
    void signup_shouldReturnTokensContainingUserId() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 1L, 3L);
        AuthResponse response = authService.signup(new SignupRequest("sauron", "mordor123"));

        int userId = jwtService.extractUserId(response.getAccessToken());
        assertNotNull(userRepository.findById(userId));
        assertEquals("sauron", userRepository.findById(userId).getUsername());
    }

    @Test
    void signup_shouldThrowWhenUsernameAlreadyExists() {
        authService.signup(new SignupRequest("sauron", "mordor123"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.signup(new SignupRequest("sauron", "other123")));
    }

    @Test
    void signin_shouldReturnValidJwtTokens() {
        JwtService jwtService = new JwtServiceImpl(SECRET, 1L, 3L);
        authService.signup(new SignupRequest("saruman", "isengard1"));

        AuthResponse response = authService.signin(new SignupRequest("saruman", "isengard1"));

        assertTrue(jwtService.isTokenValid(response.getAccessToken()));
        assertTrue(jwtService.isTokenValid(response.getRefreshToken()));
    }

    @Test
    void signin_shouldThrowWhenPasswordInvalid() {
        authService.signup(new SignupRequest("saruman", "isengard1"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.signin(new SignupRequest("saruman", "wrongpassword")));
    }

    @Test
    void signin_shouldThrowWhenUserNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.signin(new SignupRequest("unknown", "pass")));
    }
}
