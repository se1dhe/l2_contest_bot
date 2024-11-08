package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import dev.se1dhe.bot.model.enums.PrizeType;
import dev.se1dhe.bot.service.*;
import dev.se1dhe.bot.service.dbManager.EternityManager;
import dev.se1dhe.bot.service.dbManager.Lucera2DbManager;
import dev.se1dhe.bot.service.dbManager.Manager;
import dev.se1dhe.bot.service.dbManager.PainDbManager;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.IMessageHandler;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.*;

import static utils.menu.MainMenu.*;

@Service
@Log4j2
public class PrizeHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final WinnerService winnerService;
    private final RaffleService raffleService;
    private final Map<Long, Map<String, String>> userData = new HashMap<>();
    private final PrizeService prizeService;

    public PrizeHandler(DBUserService dbUserService, WinnerService winnerService, RaffleService raffleService, PrizeService prizeService) {
        this.dbUserService = dbUserService;
        this.winnerService = winnerService;
        this.raffleService = raffleService;
        this.prizeService = prizeService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());

        // Инициализация данных пользователя
        userData.putIfAbsent(dbUser.getId(), new HashMap<>());
        userData.get(dbUser.getId()).putIfAbsent("state", "INIT");

        // Обработка нажатия кнопки "reward"
        if (query.getData().equals("reward")) {
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            Page<Raffle> rafflesPage = winnerService.findUnclaimedPrizesByUser(dbUser, pageable);
            BotUtil.editMessage(
                    bot,
                    query,
                    LocalizationService.getString("start.raffleChoice", dbUser.getLang()),
                    false,
                    buildUserWinningsMenu(rafflesPage, dbUser)
            );

            userData.get(dbUser.getId()).put("state", "RAFFLE_CHOICE");
            return true;
        }

        if (query.getData().startsWith("raffle-details:") && userData.get(dbUser.getId()).get("state").equals("RAFFLE_CHOICE")) {
            userData.get(dbUser.getId()).put("raffleId", query.getData().replace("raffle-details:", ""));
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            List<Config.ServerConfig> servers = Config.getAvailableServers();
            int start = Math.min((int) pageable.getOffset(), servers.size());
            int end = Math.min((start + pageable.getPageSize()), servers.size());
            Page<Config.ServerConfig> serversPage = new PageImpl<>(servers.subList(start, end), pageable, servers.size());
            userData.get(dbUser.getId()).put("state", "SERVER_CHOICE");
            BotUtil.editMessage(bot, query, "Выберите сервер!", false, buildServerListMenu(serversPage, dbUser));
            return true;
        }

        if (query.getData().startsWith("raffle-page:")) {
            int page = Integer.parseInt(query.getData().replace("raffle-page:", ""));
            Pageable pageable = PageRequest.of(page, Config.ITEM_ON_PAGE);
            Page<Raffle> rafflesPage = winnerService.findAllByParticipantId(dbUser.getId(), pageable);
            BotUtil.editMessage(
                    bot,
                    query,
                    LocalizationService.getString("start.raffleChoice", dbUser.getLang()),
                    false,
                    buildUserWinningsMenu(rafflesPage, dbUser)
            );
            return true;
        }

        if (query.getData().startsWith("server-name:") && userData.get(dbUser.getId()).get("state").equals("SERVER_CHOICE")) {
            String serverName = query.getData().replace("server-name:", "");
            userData.get(dbUser.getId()).put("serverName", serverName);
            BotUtil.editMessage(
                    bot,
                    (Message) query.getMessage(),
                    LocalizationService.getString("start.enterCharName"),
                    true,
                    null
            );
            userData.get(dbUser.getId()).put("state", "ENTER_CHAR_NAME");
            log.info("Пользователь {} выбрал сервер: {}", dbUser.getId(), serverName);
            return true;
        }

        if (query.getData().startsWith("server-filter-page:")) {
            int page = Integer.parseInt(query.getData().replace("server-filter-page:", ""));
            Pageable pageable = PageRequest.of(page, Config.ITEM_ON_PAGE);
            List<Config.ServerConfig> servers = Config.getAvailableServers();
            int start = Math.min((int) pageable.getOffset(), servers.size());
            int end = Math.min((start + pageable.getPageSize()), servers.size());
            Page<Config.ServerConfig> serversPage = new PageImpl<>(servers.subList(start, end), pageable, servers.size());
            BotUtil.editMessage(
                    bot,
                    (Message) query.getMessage(),
                    LocalizationService.getString("server.serverListMessage"),
                    true,
                    buildServerListMenu(serversPage, dbUserService.findUserById(query.getFrom().getId()))
            );
            return true;
        }

        return false;
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException, SQLException {
        DbUser dbUser = dbUserService.registerUser(message.getFrom());

        // Обработка ввода имени персонажа
        if (userData.get(dbUser.getId()).get("state").equals("ENTER_CHAR_NAME")) {
            String charName = message.getText();

            if (charName == null || charName.isEmpty()) {
                log.warn("Пользователь {} ввел пустое имя персонажа", dbUser.getId());
                BotUtil.editMessage(bot, message, LocalizationService.getString("start.emptyField"), true, null);
                resetState(dbUser.getId());
                return true;
            }

            Manager manager = getManager(userData.get(dbUser.getId()).get("serverName"));
            if (manager == null) {
                log.error("Не удалось создать менеджер для userId={}", dbUser.getId());
                return true;
            }

            if (manager.getObjectIdByCharName(charName) == 0) {
                log.warn("Некорректное имя персонажа {} для userId={}", charName, dbUser.getId());
                BotUtil.sendMessage(BotApplication.telegramBot, message, LocalizationService.getString("start.incorrectCharName"), false, false, null);
                return true;
            }

            Raffle raffle = raffleService.getRaffleById(Long.valueOf(userData.get(dbUser.getId()).get("raffleId")));
            log.info("Обработка призов для розыгрыша {} для userId={}", raffle.getId(), dbUser.getId());
            log.info("Пользователь {} ввел ник игрового персонажа: {}", dbUser.getId(), charName);

            processPrizes(bot, message, raffle, dbUser, charName, manager);

            resetState(dbUser.getId());
            return true;
        }

        return false;
    }

    private Manager getManager(String serverName) throws SQLException {
        Config.switchServer(serverName);
        Config.ServerConfig selectedServerConfig = Config.getCurrentServerConfig();

        if (selectedServerConfig == null) {
            log.error("Не удалось найти конфигурацию для сервера: {}", serverName);
            return null;
        }

        log.info("Текущий сервер {}", selectedServerConfig.name);
        return switch (selectedServerConfig.serverType) {
            case "Lucera2" ->
                    new Lucera2DbManager(selectedServerConfig.url, selectedServerConfig.username, selectedServerConfig.password);
            case "Pain" ->
                    new PainDbManager(selectedServerConfig.url, selectedServerConfig.username, selectedServerConfig.password);
            case "L2JEternity" ->
                    new EternityManager(selectedServerConfig.url, selectedServerConfig.username, selectedServerConfig.password);
            default -> {
                log.error("Неизвестный тип сервера: {}", selectedServerConfig.serverType);
                yield null;
            }
        };
    }

    private void processPrizes(AbstractTelegramBot bot, Message message, Raffle raffle, DbUser dbUser, String charName, Manager manager) throws SQLException, TelegramApiException {
        if (prizeService.getPrizesByRaffle(raffle).isEmpty()) {
            BotUtil.sendMessage(bot, message, "Произошла ошибка! Сообщите администратору!", false, false, null);
        } else {
            List<Prize> prizeList = raffle.getPrizes();
            for (Prize prize : prizeList) {
                Winner winner = winnerService.findByPrizeIdAndParticipantId(prize.getId(), dbUser.getId());
                if (winner != null) {
                    winner.setGetPrize(true);
                    log.info("Выдача приза {} пользователю {} на сервере {} для игрового персонажа {}", prize.getItemName(), dbUser.getId(),userData.get(dbUser.getId()).get("SERVER_CHOICE"), charName);
                    if (prize.getType().equals(PrizeType.MONEY)) {
                        BotUtil.sendMessage(bot, message, String.format(LocalizationService.getString("start.moneyCongratulation"), prize.getCount(), prize.getItemName()), false, false, null);
                    } else {
                        BotUtil.sendMessage(bot, message, String.format(LocalizationService.getString("start.itemCongratulation"), prize.getCount(), prize.getItemName()), false, false, mainMenu(dbUser));
                        manager.addItem(manager.getObjectIdByCharName(charName), prize.getItemId(), prize.getCount());
                    }
                    winnerService.update(winner);
                } else {
                    log.warn("Объект winner оказался null для prizeId={} и userId={}", prize.getId(), dbUser.getId());
                }
            }
        }
    }

    private void resetState(Long userId) {
        userData.remove(userId);
        log.info("Контекст очищен для userId={}", userId);
    }
}

