package com.township.dto;

public record AuthResponse(
    String token,
    String tokenType,
    long expiresInMs,
    UserDto user
) {}
