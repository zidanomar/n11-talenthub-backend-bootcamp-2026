package com.example.user.repository;

import com.example.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser_Id(String userId);
    Optional<Address> findByIdAndUser_Id(Long id, String userId);
}
