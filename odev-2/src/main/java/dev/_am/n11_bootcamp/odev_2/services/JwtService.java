package dev._am.n11_bootcamp.odev_2.services;

public interface JwtService {

    String generateToken(int userId);
    String generateRefreshToken(int userId);
    int extractUserId(String token);
    boolean isTokenValid(String token);
    long getRefreshExpirationMs();
}
