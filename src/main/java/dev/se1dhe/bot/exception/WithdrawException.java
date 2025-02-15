package dev.se1dhe.bot.exception;

public class WithdrawException extends RuntimeException {
    public WithdrawException(String message) {
        super(message);
    }

    public WithdrawException(String message, Throwable cause) {
        super(message, cause);
    }
}