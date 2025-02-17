package dev.se1dhe.bot.exception;
public class InsufficientFundsException extends RuntimeException {
	public InsufficientFundsException(String message) {
		super(message);
	}
}