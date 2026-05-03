package com.example.user.controller;

import com.example.user.dto.AddressResponse;
import com.example.user.dto.CreateAddressRequest;
import com.example.user.dto.UserResponse;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAddress(userId, request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.ok(userService.updateAddressById(userId, addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/current")
    public ResponseEntity<UserResponse> setCurrent(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(userService.setCurrentAddress(userId, addressId));
    }
}
