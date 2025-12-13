package com.gemstore.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation. ResponseStatus;

/**
 * Exception thrown when there's a conflict with existing data.
 * Returns HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName + " already exists with " + fieldName + ": " + fieldValue);
    }
}