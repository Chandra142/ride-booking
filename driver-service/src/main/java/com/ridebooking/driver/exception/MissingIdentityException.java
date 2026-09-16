package com.ridebooking.driver.exception;

/**
 * Raised when the caller provides no identity (no X-User-Id header was
 * forwarded by the gateway, or the header is blank). Maps to HTTP 401.
 */
public class MissingIdentityException extends RuntimeException {

    public MissingIdentityException(String message) {
        super(message);
    }
}
