package com.retail.inventory.order_service.domain.exception;

import com.retail.inventory.order_service.api.dto.request.OrderItemRequest;

public class ValidationException extends RuntimeException {
    public ValidationException(Message message) {
        super(message.toString());
    }

    public ValidationException(Message message, String messageSpecifier) {
        super(message.toString() + messageSpecifier);
    }

    public enum Message {
        NO_ITEMS("Order must contain at least 1 item"),
        MISSING_SKU("Items must have SKU"),
        INVALID_QUANTITY("Quantity must be greater than 0");

        private final String message;

        Message(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
