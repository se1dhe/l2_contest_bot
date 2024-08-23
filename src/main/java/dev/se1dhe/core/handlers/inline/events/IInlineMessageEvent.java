package dev.se1dhe.core.handlers.inline.events;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;

@FunctionalInterface
public interface IInlineMessageEvent {
	/**
	 * @param event the inline message event
	 * @return {@code true} on success, {@code false} otherwise
	 * @throws TelegramApiException in case of an error
	 */
	boolean onCallbackEvent(InlineMessageEvent event) throws TelegramApiException, SQLException;
}