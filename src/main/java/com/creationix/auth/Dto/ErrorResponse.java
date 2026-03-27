package com.creationix.auth.Dto;

import org.springframework.http.HttpStatus;

public record ErrorResponse(String message, HttpStatus status) {
}
