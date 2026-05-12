package com.example.hazina.ap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    boolean existsBySupplierCode(String supplierCode);
}
