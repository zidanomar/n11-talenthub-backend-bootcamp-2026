package dev._am.n11_bootcamp.odev_2.controllers;

import dev._am.n11_bootcamp.odev_2.dto.ApiResponse;
import dev._am.n11_bootcamp.odev_2.dto.AuthResponse;
import dev._am.n11_bootcamp.odev_2.dto.SignupRequest;
import dev._am.n11_bootcamp.odev_2.dto.TokenResponse;
import dev._am.n11_bootcamp.odev_2.services.AuthService;
import dev._am.n11_bootcamp.odev_2.services.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResponse tokens = authService.signup(request);
        setRefreshCookie(response, tokens.getRefreshToken());
        long refreshExpiresAt = System.currentTimeMillis() + jwtService.getRefreshExpirationMs();
        return new ApiResponse<>(new TokenResponse(tokens.getAccessToken(), refreshExpiresAt));
    }

    @PostMapping("/signin")
    public ApiResponse<TokenResponse> signin(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResponse tokens = authService.signin(request);
        setRefreshCookie(response, tokens.getRefreshToken());
        long refreshExpiresAt = System.currentTimeMillis() + jwtService.getRefreshExpirationMs();
        return new ApiResponse<>(new TokenResponse(tokens.getAccessToken(), refreshExpiresAt));
    }

    @PostMapping("/refresh")
    public ApiResponse<String> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or missing refresh token");
        }
        int userId = jwtService.extractUserId(refreshToken);
        return new ApiResponse<>(jwtService.generateToken(userId));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletResponse response) {
        clearRefreshCookie(response);
        return new ApiResponse<>("Logged out");
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
