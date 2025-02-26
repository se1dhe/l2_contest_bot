package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.RaffleService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ILeftChatMember;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Log4j2
public class UtilHandler implements ILeftChatMember {

    private final RaffleService raffleService;

    public UtilHandler(RaffleService raffleService) {
        this.raffleService = raffleService;
    }


    @Override
    public boolean onLeftChatMember(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException {
        if (message.getChat().getId().toString().equals(Config.CHANNEL_ID)) {
            Long userId = message.getLeftChatMember().getId();
            log.info("userId {} left from chat", userId);
            raffleService.removeParticipantFromAllRaffles(userId);
            return true;
        }

        return true;
    }
}
