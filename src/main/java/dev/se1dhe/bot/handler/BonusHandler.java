package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.RaffleBonusService;
import dev.se1dhe.bot.service.RaffleService;
import dev.se1dhe.bot.service.WinnerService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.IMessageHandler;
import dev.se1dhe.core.handlers.inline.InlineUserData;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class BonusHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final RaffleService raffleService;
    private final WinnerService winnerService;
    private final RaffleBonusService raffleBonusService;
    private final Map<Long, InlineUserData> userDataMap = new HashMap<>();

    public BonusHandler(DBUserService dbUserService, RaffleService raffleService, WinnerService winnerService, RaffleBonusService raffleBonusService) {
        this.dbUserService = dbUserService;
        this.raffleService = raffleService;
        this.winnerService = winnerService;
        this.raffleBonusService = raffleBonusService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());
        if (query.getData().equals("bonus")) {
            List<Raffle> raffleList = dbUserService.getRafflesByUserId(dbUser.getId());
            for (Raffle raffle : raffleList) {
                if (winnerService.findByRaffleIdAndParticipantId(raffle.getId(), dbUser.getId()) == null) {
                    raffleBonusService.create(raffle.getId(), dbUser.getId());

                    raffleService.removeParticipant(raffle.getId(),dbUser.getId());
                    System.out.println(raffle.getName());
                }
            }
        }
        return false;
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException {
        return false;
    }
}
