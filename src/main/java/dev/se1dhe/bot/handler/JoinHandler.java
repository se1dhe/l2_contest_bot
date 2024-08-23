

package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.conf.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.service.DbUserService;
import dev.se1dhe.bot.service.LocalizationService;
import dev.se1dhe.bot.service.PrizeService;
import dev.se1dhe.bot.service.RaffleService;
import dev.se1dhe.core.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.util.BotUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.KeyboardBuilder;
import utils.Util;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

import static utils.KeyboardBuilder.button;
import static utils.Util.getEmoji;

@Service
public class JoinHandler implements ICallbackQueryHandler {

    private final RaffleService raffleService;
    private final DbUserService dbUserService;
    private final PrizeService prizeService;

    public JoinHandler(RaffleService raffleService, DbUserService dbUserService, PrizeService prizeService) {
        this.raffleService = raffleService;
        this.dbUserService = dbUserService;
        this.prizeService = prizeService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException, IOException {
        if (query.getData().startsWith("raffle_id:")) {
            Long raffleId = Long.valueOf(query.getData().replace("raffle_id:", ""));
            Raffle raffle = raffleService.getRaffleById(raffleId);

            if (raffle == null) {
                BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("raffle.notFound"), true);
                return true;
            }
            if (raffle.getRaffleResultDate().isBefore(LocalDateTime.now())) {
                BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("raffle.isEnded"), true);
                return true;
            }
            if (dbUserService.findById(query.getFrom().getId()) == null) {
                DbUser dbUser = new DbUser();
                dbUser.setId(query.getFrom().getId());
                dbUser.setUserName(query.getFrom().getFirstName());
                dbUser.setAccessLevel(0);
                dbUser.setRegDate(LocalDateTime.now());
                dbUserService.create(dbUser);
            }
            for (int i = 0; i < raffle.getParticipant().size(); i++) {
                if (Objects.equals(raffle.getParticipant().get(i).getId(), query.getFrom().getId())) {
                    BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("join.already"), Util.dateTimeParser(raffle.getRaffleResultDate())), true);
                    return true;
                }
            }
            raffle.getParticipant().add(dbUserService.findById(query.getFrom().getId()));
            raffleService.update(raffle);
            BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("join.successful"),Util.dateTimeParser(raffle.getRaffleResultDate())), true);
            if (!query.getMessage().hasPhoto()) {
                BotUtil.editMessage(bot, query.getMessage(), start(raffle), false, KeyboardBuilder.ofInline(2,
                        button(LocalizationService.getString("raffle.participation"), "raffle_id:" + raffle.getId())
                ));
            }
            else
                BotUtil.editMessageCaption(bot, query, startDelay(raffle), false, KeyboardBuilder.ofInline(2,
                        button(LocalizationService.getString("raffle.participation"), "raffle_id:" + raffle.getId())
                ));


        }
        return false;
    }

    private String start(Raffle raffle) {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizeService.getPrizesByRaffle(raffle)) {
            s.append(getEmoji(prize.getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(prize.getCount()).append(" ").append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"),Config.BONUS_ITEM_COUNT,Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }
        return String.format(LocalizationService.getString("raffle.startMessage"), raffle.getSiteUrl(), raffle.getId(), raffle.getDesc(), s, raffle.getParticipant().size(), raffle.getWinnerCount(), raffle.getSiteUrl(), raffle.getSiteUrl(),raffle.getChannelForSub(), Util.dateTimeParser(raffle.getRaffleResultDate()));
    }

    private String startDelay(Raffle raffle) {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizeService.getPrizesByRaffle(raffle)) {
            s.append(getEmoji(prize.getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(prize.getCount()).append(" ").append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"),Config.BONUS_ITEM_COUNT,Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }
        return String.format(LocalizationService.getString("delay.startMessage"), raffle.getName(), raffle.getDesc(), s, raffle.getParticipant().size(), raffle.getWinnerCount(), raffle.getSiteUrl(), raffle.getSiteUrl(), raffle.getChannelForSub(), Util.dateTimeParser(raffle.getRaffleResultDate()));
    }
}
