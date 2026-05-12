package com.example.hazina.ar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    boolean existsByCustomerCode(String customerCode);
    Optional<Customer> findByCustomerCode(String customerCode);
    List<Customer> findByActiveOrderByNameAsc(boolean active);
}
