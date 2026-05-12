package com.example.hazina.ap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBillRequest(
        @NotNull UUID supplierId,
        @NotNull UUID apAccountId,
        @NotNull LocalDate billDate,
        @NotNull LocalDate dueDate,
        String supplierRef,
        String notes,
        @NotEmpty @Valid List<BillLineRequest> lines
) {}
