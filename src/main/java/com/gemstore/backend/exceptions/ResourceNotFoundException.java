package com. gemstore.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web. bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Returns HTTP 404 Not Found. 
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName + " not found with " + fieldName + ": " + fieldValue);
    }
}