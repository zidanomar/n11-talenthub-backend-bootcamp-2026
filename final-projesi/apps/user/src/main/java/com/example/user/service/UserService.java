package com.example.user.service;

import com.example.user.dto.AddressResponse;
import com.example.user.dto.ChangePasswordRequest;
import com.example.user.dto.CreateAddressRequest;
import com.example.user.dto.SigninRequest;
import com.example.user.dto.SignupRequest;
import com.example.user.dto.SignupResponse;
import com.example.user.dto.UpdateAddressRequest;
import com.example.user.dto.UserResponse;

import java.util.List;
import java.util.Map;

public interface UserService {
    SignupResponse signup(SignupRequest request);
    Map<String, Object> signin(SigninRequest request);
    Map<String, Object> refresh(String refreshToken);
    UserResponse getById(String userId);

    List<AddressResponse> getAddresses(String userId);
    AddressResponse createAddress(String userId, CreateAddressRequest request);
    AddressResponse updateAddressById(String userId, Long addressId, CreateAddressRequest request);
    void deleteAddress(String userId, Long addressId);
    UserResponse setCurrentAddress(String userId, Long addressId);
    UserResponse updateAddress(String userId, UpdateAddressRequest request);

    void changePassword(String userId, ChangePasswordRequest request);
}
