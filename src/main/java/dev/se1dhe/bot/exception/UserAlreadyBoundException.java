package dev.se1dhe.bot.exception;

public class UserAlreadyBoundException extends RuntimeException {
    public UserAlreadyBoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserAlreadyBoundException(String message) {
    }
}