package dev._am.n11_bootcamp.odev_2.services.impl;

import dev._am.n11_bootcamp.odev_2.services.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String secret;
    private final long expiration;
    private final long refreshExpiration;

    public JwtServiceImpl(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expiration,
            @Value("${app.jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.secret = secret;
        this.expiration = expiration * 1000;
        this.refreshExpiration = refreshExpiration * 1000;
    }

    @Override
    public String generateToken(int userId) {
        return buildToken(userId, expiration);
    }

    @Override
    public String generateRefreshToken(int userId) {
        return buildToken(userId, refreshExpiration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public int extractUserId(String token) {
        Object data = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("payload");
        return ((Number) ((java.util.Map<String, Object>) data).get("userId")).intValue();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public long getRefreshExpirationMs() {
        return refreshExpiration;
    }

    private String buildToken(int userId, long expiration) {
        return Jwts.builder()
                .claim("payload", java.util.Map.of("userId", userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
