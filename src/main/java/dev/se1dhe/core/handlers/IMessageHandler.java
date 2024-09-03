package dev.se1dhe.core.handlers;

import dev.se1dhe.core.bots.AbstractTelegramBot;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;


public interface IMessageHandler extends ITelegramHandler {
	/**
	 * Fired whenever user types anything but a command
	 *
	 * @param bot     the bot
	 * @param update  the update
	 * @param message the message
	 * @return {@code true} if handler 'consumed' that event, aborting notification to other handlers, {@code false} otherwise, continuing to look for handler that would return {@code true}
	 * @throws TelegramApiException the exception
	 */
	boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException, SQLException;
}
