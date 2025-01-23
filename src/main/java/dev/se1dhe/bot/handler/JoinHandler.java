package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.GameUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.service.*;
import dev.se1dhe.bot.service.dbManager.EternityManager;
import dev.se1dhe.bot.service.dbManager.Lucera2DbManager;
import dev.se1dhe.bot.service.dbManager.Manager;
import dev.se1dhe.bot.service.dbManager.PainDbManager;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.KeyboardBuilder;
import utils.Util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static utils.Util.getEmoji;

@Service
@Log4j2
public class JoinHandler implements ICallbackQueryHandler {

    private final RaffleService raffleService;
    private final DBUserService dbUserService;
    private final PrizeService prizeService;
    private final GameUserService gameUserService;

    @Autowired
    public JoinHandler(RaffleService raffleService, DBUserService dbUserService, PrizeService prizeService, GameUserService gameUserService) {
        this.raffleService = raffleService;
        this.dbUserService = dbUserService;
        this.prizeService = prizeService;
        this.gameUserService = gameUserService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException, SQLException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());
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
            if (gameUserService.findByDbUserAndActive(dbUser,true)==null||gameUserService.findByDbUserAndActive(dbUser,true).isEmpty()) {
                BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("join.noCharacter"), true);
                return true;
            }

            if (!hasRequiredStageId(dbUser, raffle.getStageId())) {
                BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("join.wrongMinimumStageId"),raffle.getStageId()), true);
                return true;
            }

            for (int i = 0; i < raffle.getParticipant().size(); i++) {
                if (Objects.equals(raffle.getParticipant().get(i).getId(), query.getFrom().getId())) {
                    BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("join.already"), Util.dateTimeParser(raffle.getRaffleResultDate())), true);
                    return true;
                }
            }

            raffle.getParticipant().add(dbUser);
            raffleService.update(raffle);
            log.info("Пользователь {} зарегистрирован в конкурсе {}", dbUser.getId(), raffle.getId());
            BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("join.successful"), Util.dateTimeParser(raffle.getRaffleResultDate())), true);
            Message message = (Message) query.getMessage();
            if (!message.hasPhoto()) {
                BotUtil.editMessage(bot, (Message) query.getMessage(), start(raffle), true, KeyboardBuilder.inline()
                        .button(LocalizationService.getString("raffle.participation"), "raffle_id:" + raffle.getId())
                        .build());
            } else {
                BotUtil.editMessageCaption(bot, query, startDelay(raffle), KeyboardBuilder.inline()
                        .button(LocalizationService.getString("raffle.participation"), "raffle_id:" + raffle.getId())
                        .build());
            }


        }
        return false;
    }

    private boolean hasRequiredStageId(DbUser dbUser, int requiredStageId) throws SQLException {
        List<GameUser> linkedCharacters = gameUserService.findByDbUserAndActive(dbUser,true);
        if (linkedCharacters == null || linkedCharacters.isEmpty()) {
            return false;
        }
        for (GameUser character : linkedCharacters) {
            Config.ServerConfig currentServerConfig = Config.getAvailableServers().stream().filter(serverConfig -> serverConfig.name.equals(character.getServerName())).findFirst().orElse(null);
            if (currentServerConfig == null) {
                log.error("Не удалось найти конфигурацию для сервера: {}", character.getServerName());
                continue;
            }
            Manager manager = getManager(currentServerConfig);
            if (manager == null) {
                log.error("Не удалось создать менеджер для сервера {}", character.getServerName());
                continue;
            }
            try {
                // Передаем charId как Long
                List<Integer> stageIds = manager.getStageIdsByCharId(Math.toIntExact(character.getCharId()));
                for (Integer stageId : stageIds) {
                    if (stageId >= requiredStageId) {
                        System.out.println(stageId);
                        return true;
                    }
                }
            } finally {
                manager.close();
            }
        }

        return false;
    }

    private Manager getManager(Config.ServerConfig serverConfig) throws SQLException {
        return switch (serverConfig.serverType) {
            case "Lucera2" ->
                    new Lucera2DbManager(serverConfig.url, serverConfig.username, serverConfig.password);
            case "Pain" ->
                    new PainDbManager(serverConfig.url, serverConfig.username, serverConfig.password);
            case "L2JEternity" ->
                    new EternityManager(serverConfig.url, serverConfig.username, serverConfig.password);
            default -> {
                log.error("Неизвестный тип сервера: {}", serverConfig.serverType);
                yield null;
            }
        };
    }

    private String start(Raffle raffle) {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizeService.getPrizesByRaffle(raffle)) {
            s.append(getEmoji(prize.getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(prize.getCount()).append(" ").append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"), Config.BONUS_ITEM_COUNT, Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }
        return String.format(LocalizationService.getString("raffle.startMessage"), raffle.getSiteUrl(), raffle.getId(), raffle.getDescription(), s, raffle.getParticipant().size(), raffle.getWinnerCount(), raffle.getSiteUrl(), raffle.getSiteUrl(), raffle.getChannelForSub(), Util.dateTimeParser(raffle.getRaffleResultDate()));
    }

    private String startDelay(Raffle raffle) {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizeService.getPrizesByRaffle(raffle)) {
            s.append(getEmoji(prize.getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(prize.getCount()).append(" ").append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"), Config.BONUS_ITEM_COUNT, Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }
        return String.format(LocalizationService.getString("delay.startMessage"), raffle.getName(), raffle.getDescription(), s, raffle.getParticipant().size(), raffle.getWinnerCount(), raffle.getSiteUrl(), raffle.getSiteUrl(), raffle.getChannelForSub(), Util.dateTimeParser(raffle.getRaffleResultDate()));
    }
}