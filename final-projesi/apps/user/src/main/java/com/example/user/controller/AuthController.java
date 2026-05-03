package com.example.user.controller;

import com.example.user.dto.SigninRequest;
import com.example.user.dto.SignupRequest;
import com.example.user.dto.SignupResponse;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signup(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<Map<String, Object>> signin(@Valid @RequestBody SigninRequest request) {
        return ResponseEntity.ok(userService.signin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(userService.refresh(request.refreshToken()));
    }

    record RefreshRequest(String refreshToken) {}
}
