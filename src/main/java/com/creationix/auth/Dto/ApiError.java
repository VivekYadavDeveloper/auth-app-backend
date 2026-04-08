package com.creationix.auth.Dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(
        int status,
        String path,
        String error,
        String message,
        OffsetDateTime timestamp
) {
    public static ApiError of(int status, String path, String error, String message) {
        return new ApiError(status, path, error, message, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
