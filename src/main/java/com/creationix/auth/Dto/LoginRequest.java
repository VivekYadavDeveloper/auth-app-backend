package com.creationix.auth.Dto;

public record LoginRequest(
        String email,
        String password
) {
}
