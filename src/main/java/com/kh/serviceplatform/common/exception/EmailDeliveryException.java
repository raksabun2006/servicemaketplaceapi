package com.kh.serviceplatform.common.exception;

/**
 * Dedicated exception thrown when an email cannot be prepared or delivered via SMTP.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
