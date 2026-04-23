package dev._am.n11_bootcamp.odev_2.services;

import dev._am.n11_bootcamp.odev_2.services.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private JwtService jwtService;

    private static final String SECRET = "c2VjcmV0LWtleS1mb3ItdGVzdC1wdXJwb3Nlcy1vbmx5LTMyY2hhcg==";
    private static final long EXPIRATION = 1L;
    private static final long REFRESH_EXPIRATION = 3L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    void generateToken_shouldReturnJwtWithThreeParts() {
        String token = jwtService.generateToken(1);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateRefreshToken_shouldReturnJwtWithThreeParts() {
        String token = jwtService.generateRefreshToken(1);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateToken_shouldReturnDifferentTokensForDifferentUsers() {
        assertNotEquals(jwtService.generateToken(1), jwtService.generateToken(2));
    }

    @Test
    void accessAndRefreshTokens_shouldBeDifferent() {
        assertNotEquals(jwtService.generateToken(1), jwtService.generateRefreshToken(1));
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(1);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForGarbageToken() {
        assertFalse(jwtService.isTokenValid("not.a.token"));
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken(42);
        assertEquals(42, jwtService.extractUserId(token));
    }
}
