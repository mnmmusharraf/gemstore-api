package com.gemstore.backend.exceptions;

import com.gemstore.backend.dtos.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /* ===================== Existing ===================== */

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /* ===================== Authentication (401) ===================== */

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked(LockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Account is locked"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        log.warn("Account disabled: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Account is not active"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed"));
    }

    /* ===================== Authorization (403) ===================== */

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    /* ===================== Validation ===================== */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /* ===================== Method Not Allowed (405) ===================== */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Method not allowed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method not allowed: " + ex.getMethod()));
    }

    /* ===================== Generic Fallback ===================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}