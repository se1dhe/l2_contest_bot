package dev.se1dhe.core.util;

import dev.se1dhe.core.handlers.ICommandHandler;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;


public class BotUtil {
    public static <T extends TelegramClient> void sendAction(T bot, Message message, ActionType actionType) throws TelegramApiException {
        final SendChatAction sendAction = SendChatAction.builder().
                chatId(message.getChat().getId()).
                action(actionType.toString())
                .build();
        bot.execute(sendAction);
    }

    public static <T extends TelegramClient> void sendUsage(T bot, Message message, ICommandHandler handler) throws TelegramApiException {
        final SendMessage msg = SendMessage.builder().
                chatId(message.getChat().getId()).
                text(handler.getUsage()).
                build();
        bot.execute(msg);
    }

    public static <T extends TelegramClient> Message sendMessage(T bot, Message message, String text, boolean replyToMessage, boolean useHtml, ReplyKeyboard replayMarkup) throws TelegramApiException {
        final SendMessage msg = SendMessage.builder().
                chatId(message.getChat().getId()).
                text(text).
                parseMode(useHtml ? ParseMode.HTML : null).
                replyToMessageId(replyToMessage ? message.getMessageId() : null).
                replyMarkup(replayMarkup).
                build();
        return bot.execute(msg);
    }

    public static <T extends TelegramClient> void sendHtmlMessage(T bot, Message message, String text, boolean replyToMessage, ReplyKeyboard replayMarkup) throws TelegramApiException {
        final SendMessage msg = SendMessage.builder().
                chatId(message.getChat().getId()).
                text(text).
                parseMode(ParseMode.HTML).
                replyToMessageId(replyToMessage ? message.getMessageId() : null).
                replyMarkup(replayMarkup).
                build();
        bot.execute(msg);
    }

    public static <T extends TelegramClient> void editMessage(T bot, Message message, String text, boolean useMarkDown, InlineKeyboardMarkup inlineMarkup) throws TelegramApiException {
        final EditMessageText msg = EditMessageText.builder().
                chatId(message.getChat().getId()).
                messageId(message.getMessageId()).
                text(text).
                parseMode(useMarkDown ? ParseMode.HTML : null).
                replyMarkup(inlineMarkup).
                build();
        ;bot.execute(msg);
    }

    public static <T extends TelegramClient> void editMessage(T bot, CallbackQuery query, String text, boolean useMarkDown, InlineKeyboardMarkup inlineMarkup) throws TelegramApiException {
        final EditMessageText msg = EditMessageText.builder().
                chatId(query.getMessage().getChat().getId()).
                messageId(query.getMessage().getMessageId()).
                inlineMessageId(query.getInlineMessageId()).
                text(text).
                parseMode(useMarkDown ? ParseMode.MARKDOWNV2 : null).
                replyMarkup(inlineMarkup).
                build();
        bot.execute(msg);
    }

    public static <T extends TelegramClient> void sendAnswerCallbackQuery(T bot, CallbackQuery query, String answer, boolean showAlert) throws TelegramApiException {
        final AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(query.getId());
        answerCallbackQuery.setCallbackQueryId(query.getId());
        answerCallbackQuery.setShowAlert(showAlert);
        answerCallbackQuery.setText(answer);
        bot.execute(answerCallbackQuery);
    }

    public static <T extends TelegramClient> void editMessageCaption(T bot, CallbackQuery query, String text, InlineKeyboardMarkup inlineMarkup) throws TelegramApiException {
        final EditMessageCaption msg = new EditMessageCaption();
        msg.setChatId(Long.toString(query.getMessage().getChat().getId()));
        msg.setMessageId(query.getMessage().getMessageId());
        msg.setInlineMessageId(query.getInlineMessageId());
        msg.setCaption(text);
        msg.setParseMode(ParseMode.HTML);
        msg.setReplyMarkup(inlineMarkup);
        bot.execute(msg);
    }

    public static <T extends TelegramClient> void sendHtmlMessageById(T bot, String chatId, String text, ReplyKeyboard replayMarkup) throws TelegramApiException {
        final SendMessage msg = new SendMessage(chatId,text);
        msg.enableHtml(true);
        msg.setParseMode(ParseMode.HTML);
        msg.disableWebPagePreview();
        if (replayMarkup != null) {
            msg.setReplyMarkup(replayMarkup);
        }
        bot.execute(msg);
    }

    public static <T extends TelegramClient> Message sendPhotoById(T bot, String messageToId, String text, File file, ReplyKeyboard replayMarkup) throws TelegramApiException {
        final SendPhoto photo = new SendPhoto(messageToId,new InputFile(file));
        photo.setCaption(text);
        photo.setReplyMarkup(replayMarkup);
        photo.setParseMode("HTML");
        return bot.execute(photo);
    }
}
