package utils;

import org.telegram.telegrambots.meta.api.objects.LoginUrl;
import org.telegram.telegrambots.meta.api.objects.games.CallbackGame;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.LinkedList;
import java.util.List;

public final class KeyboardBuilder {

    private KeyboardBuilder() {
    }

    /**
     * Creates a builder for {@link InlineKeyboardMarkup}
     *
     * @return An instance of {@link InlineKeyboardBuilder}
     */
    public static InlineKeyboardBuilder inline() {
        return new InlineKeyboardBuilder();
    }

    /**
     * Creates a builder for {@link ReplyKeyboardMarkup}
     *
     * @return An instance of {@link ReplyKeyboardBuilder}
     */
    public static ReplyKeyboardBuilder reply() {
        return new ReplyKeyboardBuilder();
    }

    public static final class InlineKeyboardBuilder {

        private List<List<InlineKeyboardButton>> keyboard = new LinkedList<>();
        private List<InlineKeyboardButton> row = new LinkedList<>();

        private InlineKeyboardBuilder() {
        }

        public InlineKeyboardBuilder button(String text, String callbackData) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setCallbackData(callbackData);
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder buttonLogin(String text, String loginUrl) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setLoginUrl(new LoginUrl(loginUrl));
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder buttonUrl(String text, String url) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setUrl(url);
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder buttonSwitch(String text, String switchInlineQuery) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setSwitchInlineQuery(switchInlineQuery);
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder buttonSwitchCurrent(String text, String switchInlineQueryCurrentChat) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setSwitchInlineQueryCurrentChat(switchInlineQueryCurrentChat);
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder buttonCallbackGame(String text) {
            InlineKeyboardButton button = new InlineKeyboardButton(text);
            button.setCallbackGame(new CallbackGame());
            row.add(button);
            return this;
        }

        public InlineKeyboardBuilder row() {
            if (!row.isEmpty()) {
                keyboard.add(row);
                row = new LinkedList<>();
            }
            return this;
        }

        public InlineKeyboardMarkup build() {
            row();
            List<InlineKeyboardRow> keyboardRows = new LinkedList<>();
            for (List<InlineKeyboardButton> row : keyboard) {
                keyboardRows.add(new InlineKeyboardRow(row));
            }
            return new InlineKeyboardMarkup(keyboardRows);
        }
    }

    public static final class ReplyKeyboardBuilder {

        private List<KeyboardRow> keyboard = new LinkedList<>();
        private KeyboardRow row = new KeyboardRow();

        private ReplyKeyboardBuilder() {
        }

        public ReplyKeyboardBuilder button(String text) {
            row.add(text);
            return this;
        }

        public ReplyKeyboardBuilder row() {
            if (!row.isEmpty()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
            return this;
        }

        public ReplyKeyboardMarkup build() {
            row();
            return new ReplyKeyboardMarkup(keyboard);
        }
    }
}