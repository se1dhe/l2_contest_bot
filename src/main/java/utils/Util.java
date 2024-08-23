package utils;

import dev.se1dhe.bot.service.LocalizationService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Log4j2
public class Util {

    public static String dateTimeParser(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return localDateTime.format(formatter);
    }

    public static String getEmoji(int number) {
        String[] num = new String[]{"1️⃣ ", "\n2️⃣ ", "\n3️⃣ ", "\n4️⃣ ", "\n5️⃣ ", "\n6️⃣ ", "\n7️⃣ ", "\n8️⃣ ", "\n9️⃣ ", "\n\uD83D\uDD1F ", "\n1️⃣1️⃣ ", "\n1️⃣2️⃣ ", "\n1️⃣3️⃣ ", "\n1️⃣4️⃣ ", "\n1️⃣5️⃣ ", "\n1️⃣6️⃣ ", "\n1️⃣7️⃣ ", "\n1️⃣8️⃣ ", "\n1️⃣9️⃣ ", "\n2️⃣0️⃣ "};
        return num[number-1];
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd.yyyy HH:mm");
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    public  static boolean isMember(AbstractTelegramBot bot, String channel, long userId) {
        GetChatMember member = new GetChatMember(channel, userId);
        try {
            ChatMember res = bot.execute(member);
            if (res.getStatus().contains("left") || res.getStatus().equals("kicked")) {
                return false;
            } else {
                return true;
            }
        } catch (TelegramApiException e) {
            log.warn("no access {}" , channel);
        }
        return false;
    }

    /**
     * Метод для экранирования символов в строке, которые могут вызвать ошибки при отправке сообщений через Telegram API.
     *
     * @param text Текст, который нужно экранировать.
     * @return Экранированный текст.
     */
    public static String escapeTelegramReservedCharacters(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("!", "\\!");
    }
}
