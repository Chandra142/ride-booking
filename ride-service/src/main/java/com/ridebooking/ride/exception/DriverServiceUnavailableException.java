package com.ridebooking.ride.exception;

public class DriverServiceUnavailableException extends RuntimeException {
    public DriverServiceUnavailableException(String message) {
        super(message);
    }
}
