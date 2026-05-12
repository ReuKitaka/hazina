package com.example.hazina.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String role
) {}
