package com.retail.inventory.order_service.domain.exception;

public class RetryException extends RuntimeException {
    public RetryException(Message message) {
        super(message.toString());
    }

    public enum Message {
      ORIGIN_ORDER_NOT_FOUND("Retry order references non-existent Order ID");

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
