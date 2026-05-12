package com.example.hazina.ar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID customerId,
        @NotNull UUID arAccountId,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        String notes,
        @NotEmpty @Valid List<InvoiceLineRequest> lines
) {}
