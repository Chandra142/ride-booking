package com.ridebooking.driver.exception;

/**
 * Raised when coordinates are null, NaN, infinite or out of range.
 * Maps to HTTP 400 via {@link GlobalExceptionHandler}.
 */
public class InvalidCoordinatesException extends RuntimeException {

    public InvalidCoordinatesException(String message) {
        super(message);
    }
}
