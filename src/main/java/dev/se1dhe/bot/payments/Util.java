package dev.se1dhe.bot.payments;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Log4j2
public class Util {
    public static String formatTimestamp(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String formattedDate = formatter.format(Instant.ofEpochSecond(timestamp));
        log.debug("Отформатированная дата: {}", formattedDate);
        return formattedDate;
    }
}
