package com.creationix.auth.Exception;

import com.creationix.auth.Dto.ApiError;
import com.creationix.auth.Dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*USER AUTHENTICATION EXCEPTION*/
    @ExceptionHandler({
            UsernameNotFoundException.class,
            
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiError> handleAuthException(Exception e, HttpServletRequest request) {
        log.info("Exception Class: {}", e.getClass().getName());
        ApiError apiError = ApiError.of(HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), "Bad Request", e.getCause().getMessage());
        return ResponseEntity.badRequest().body(apiError);
    }

    /*RESOURCE NOT FOUND EXCEPTION HANDLER*/
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse internalServerError = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(internalServerError);
    }

    /*BAD CREDENTIAL*/
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse internalServerError = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(internalServerError);
    }

}
