// exceptions/UnauthorizedException.java
package com.gemstore.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web. bind.annotation.ResponseStatus;

/**
 * Exception thrown when user is not authorized to perform an action.
 * Returns HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("You are not authorized to perform this action");
    }
}