package dev._am.n11_bootcamp.odev_2.services;

import dev._am.n11_bootcamp.odev_2.dto.AuthResponse;
import dev._am.n11_bootcamp.odev_2.dto.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse signin(SignupRequest request);
}
