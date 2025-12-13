package com.gemstore.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web. bind.annotation.ResponseStatus;

/**
 * Exception thrown when request data is invalid.
 * Returns HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}