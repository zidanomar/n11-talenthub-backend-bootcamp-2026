package dev._am.n11_bootcamp.odev_2.services.impl;

import dev._am.n11_bootcamp.odev_2.domain.User;
import dev._am.n11_bootcamp.odev_2.dto.AuthResponse;
import dev._am.n11_bootcamp.odev_2.dto.SignupRequest;
import dev._am.n11_bootcamp.odev_2.repositories.UserRepository;
import dev._am.n11_bootcamp.odev_2.services.AuthService;
import dev._am.n11_bootcamp.odev_2.services.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User saved = userRepository.save(new User(request.getUsername(), hashedPassword));

        return buildAuthResponse(saved.getId());
    }

    @Override
    public AuthResponse signin(SignupRequest request) {
        User user = userRepository.findByUsername(request.getUsername());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return buildAuthResponse(user.getId());
    }

    private AuthResponse buildAuthResponse(int userId) {
        String accessToken = jwtService.generateToken(userId);
        String refreshToken = jwtService.generateRefreshToken(userId);
        return new AuthResponse(accessToken, refreshToken);
    }
}
