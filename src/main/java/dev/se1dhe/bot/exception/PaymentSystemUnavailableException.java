package dev.se1dhe.bot.exception;

public class PaymentSystemUnavailableException extends RuntimeException {
    public PaymentSystemUnavailableException(String message) {
        super(message);
    }

    public PaymentSystemUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}