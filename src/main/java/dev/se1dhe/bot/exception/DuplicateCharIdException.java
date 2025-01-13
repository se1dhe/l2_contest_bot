package dev.se1dhe.bot.exception;

public class DuplicateCharIdException extends RuntimeException {
    public DuplicateCharIdException(String message, Throwable cause) {
        super(message, cause);
    }
}