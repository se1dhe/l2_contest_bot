package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
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
import utils.menu.MainMenu;

import java.sql.SQLException;
import java.util.*;

import static utils.menu.MainMenu.buildNonWinningRafflesMenu;
import static utils.menu.MainMenu.buildServerListMenu;

@Service
@Log4j2
public class BonusHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final WinnerService winnerService;
    private final RaffleService raffleService;
    private final RaffleBonusService raffleBonusService;
    private final Map<Long, Map<String, String>> userData = new HashMap<>();

    public BonusHandler(DBUserService dbUserService, WinnerService winnerService, RaffleService raffleService, RaffleBonusService raffleBonusService) {
        this.dbUserService = dbUserService;
        this.winnerService = winnerService;
        this.raffleService = raffleService;
        this.raffleBonusService = raffleBonusService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());

        userData.putIfAbsent(dbUser.getId(), new HashMap<>());
        userData.get(dbUser.getId()).putIfAbsent("state", "INIT");

        if (query.getData().equals("bonus")) {
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            Page<Raffle> rafflesPage = raffleService.findNonWinningRafflesByUser(dbUser, pageable);
            BotUtil.editMessage(
                    bot,
                    query,
                    LocalizationService.getString("bonus.choiceContest", dbUser.getLang()),
                    false,
                    buildNonWinningRafflesMenu(rafflesPage, dbUser)
            );

            userData.get(dbUser.getId()).put("state", "NON_WINNING_RAFFLE_CHOICE");
            return true;
        }

        // Обработка нажатия кнопки с деталями розыгрыша
        if (query.getData().startsWith("bonus-raffle-details:") && userData.get(dbUser.getId()).get("state").equals("NON_WINNING_RAFFLE_CHOICE")) {
            userData.get(dbUser.getId()).put("raffleId", query.getData().replace("bonus-raffle-details:", ""));
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            List<Config.ServerConfig> servers = Config.getAvailableServers();
            int start = Math.min((int) pageable.getOffset(), servers.size());
            int end = Math.min((start + pageable.getPageSize()), servers.size());
            Page<Config.ServerConfig> serversPage = new PageImpl<>(servers.subList(start, end), pageable, servers.size());
            userData.get(dbUser.getId()).put("state", "BONUS_SERVER_CHOICE");
            BotUtil.editMessage(bot, query, "Выберите сервер!", false, buildServerListMenu(serversPage, dbUser));
            return true;
        }



        // Обработка нажатия кнопки с выбором сервера
        if (query.getData().startsWith("server-name:") && userData.get(dbUser.getId()).get("state").equals("BONUS_SERVER_CHOICE")) {
            String bonusServerName = query.getData().replace("server-name:", "");
            userData.get(dbUser.getId()).put("bonusServerName", bonusServerName);
            BotUtil.editMessage(
                    bot,
                    (Message) query.getMessage(),
                    LocalizationService.getString("start.enterCharName"),
                    true,
                    null
            );
            userData.get(dbUser.getId()).put("state", "BONUS_ENTER_CHAR_NAME");
            return true;
        }

        // Обработка нажатия кнопки со страницей фильтрации серверов
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
        if (userData.get(dbUser.getId()).get("state").equals("BONUS_ENTER_CHAR_NAME")) {
            String charName = message.getText();

            if (charName == null || charName.isEmpty()) {
                log.warn("Пользователь {} ввел пустое имя персонажа", dbUser.getId());
                BotUtil.editMessage(bot, message, LocalizationService.getString("start.emptyField"), true, null);
                resetState(dbUser.getId());
                return true;
            }

            Manager manager = getManager(userData.get(dbUser.getId()).get("bonusServerName"));
            if (manager == null) {
                log.error("Не удалось создать менеджер для userId={}", dbUser.getId());
                return true;
            }

            if (manager.getObjectIdByCharName(charName) == 0) {
                log.warn("Некорректное имя персонажа {} для userId={}", charName, dbUser.getId());
                BotUtil.sendMessage(BotApplication.telegramBot, message, LocalizationService.getString("start.incorrectCharName"), false, false, null);
                return true;
            }


            if (!Config.DAILY_PARTICIPANT_BONUS) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("start.bonusDisable"), false, false, null);
                return true;
            }

            String s = LocalizationService.getString("start.bonusDisable");
            if (Config.ITEM_ENABLE) {
                s = LocalizationService.getString("start.bonusCongratulation");
                manager.addItem(manager.getObjectIdByCharName(charName), Config.BONUS_ITEM_ID, Config.BONUS_ITEM_COUNT);
            }
            if (Config.PREMIUM_ENABLE) {
                s = LocalizationService.getString("start.bonusCongratulation");
                manager.addPremiumData(manager.getObjectIdByCharName(charName), Config.PREMIUM_HOUR);
            }
            BotUtil.sendMessage(BotApplication.telegramBot, message, s, false, false, MainMenu.mainMenu(dbUser));
            raffleBonusService.create(Long.valueOf(userData.get(dbUser.getId()).get("raffleId")), dbUser.getId());
            raffleService.removeParticipant(Long.valueOf(userData.get(dbUser.getId()).get("raffleId")), dbUser.getId());
            resetState(dbUser.getId());
            return true;
        }

        return false;
    }

    /**
     * Возвращает экземпляр менеджера базы данных в зависимости от выбранного пользователем сервера
     *
     * @param serverName Название выбранного сервера
     * @return Менеджер базы данных
     * @throws SQLException Исключение при работе с базой данных
     */
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


    private void resetState(Long userId) {
        userData.remove(userId);
        log.info("Контекст очищен для userId={}", userId);
    }
}
