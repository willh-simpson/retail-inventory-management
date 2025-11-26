package com.retail.inventory.order_service.api.controller.advice;

import com.retail.inventory.common.messaging.InventoryReservationStatus;
import com.retail.inventory.order_service.domain.exception.ReservationFailedException;
import com.retail.inventory.order_service.domain.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(ReservationFailedException.class)
    public ResponseEntity<?> handleReservationFailed(ReservationFailedException ex) {
        String message = ex.getMessage();
        InventoryReservationStatus status = InventoryReservationStatus.ERROR;

        if (message.toLowerCase().contains("insufficient")) {
            status = InventoryReservationStatus.INSUFFICIENT_STOCK;
        } else if (message.toLowerCase().contains("out")) {
            status = InventoryReservationStatus.OUT_OF_STOCK;
        } else if (message.toLowerCase().contains("discontinued")) {
            status = InventoryReservationStatus.DISCONTINUED;
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", status,
                        "message", message
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationFailed(ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "validation failed",
                        "message", ex.getMessage()
                ));
    }
}
