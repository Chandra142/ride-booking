package com.ridebooking.driver.exception;

/**
 * Raised when an authenticated user attempts an operation on a driver they
 * do not own (e.g. a rider, or a different driver, calls PUT
 * /api/v1/drivers/{otherDriverId}/location). Maps to HTTP 403.
 */
public class OwnershipViolationException extends RuntimeException {

    public OwnershipViolationException(String message) {
        super(message);
    }
}
