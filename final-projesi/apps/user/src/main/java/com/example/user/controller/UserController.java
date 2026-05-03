package com.example.user.controller;

import com.example.user.dto.ChangePasswordRequest;
import com.example.user.dto.UpdateAddressRequest;
import com.example.user.dto.UserResponse;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PatchMapping("/me/address")
    public ResponseEntity<UserResponse> updateAddress(@RequestHeader("X-User-Id") String userId,
                                                      @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(userId, request));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestHeader("X-User-Id") String userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
