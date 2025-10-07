package com.retail.inventory.order_service.domain.exception;

public class ReservationFailedException extends RuntimeException {
    public ReservationFailedException(String message) {
        super(message);
    }

    public ReservationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
