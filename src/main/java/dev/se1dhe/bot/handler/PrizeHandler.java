package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import dev.se1dhe.bot.model.enums.PrizeType;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.LocalizationService;
import dev.se1dhe.bot.service.WinnerService;
import dev.se1dhe.bot.service.dbManager.EternityManager;
import dev.se1dhe.bot.service.dbManager.Lucera2DbManager;
import dev.se1dhe.bot.service.dbManager.Manager;
import dev.se1dhe.bot.service.dbManager.PainDbManager;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.ICommandHandler;
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

import static utils.menu.MainMenu.buildServerListMenu;
import static utils.menu.MainMenu.buildUserWinningsMenu;

@Service
@Log4j2
public class PrizeHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final WinnerService winnerService;

    private final Map<Long, Map<String, String>> userData = new HashMap<>();
    private static String WINNER_ID_FIELD;


    public PrizeHandler(DBUserService dbUserService, WinnerService winnerService) {
        this.dbUserService = dbUserService;
        this.winnerService = winnerService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());

        userData.putIfAbsent(dbUser.getId(), new HashMap<>());
        userData.get(dbUser.getId()).putIfAbsent("state", "INIT");

        if (query.getData().equals("reward") && userData.get(dbUser.getId()).get("state").equals("INIT") ) {
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            Page<Raffle> rafflesPage = winnerService.findAllByParticipantId(dbUser.getId(), pageable);

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

        if (query.getData().startsWith("raffle-details:")&& userData.get(dbUser.getId()).get("state").equals("RAFFLE_CHOICE") ) {
            userData.get(dbUser.getId()).put("raffleId", query.getData().replace("raffle-details:", ""));
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            List<Config.ServerConfig> servers = Config.getAvailableServers();
            int start = Math.min((int) pageable.getOffset(), servers.size());
            int end = Math.min((start + pageable.getPageSize()), servers.size());
            Page<Config.ServerConfig> serversPage = new PageImpl<>(servers.subList(start, end), pageable, servers.size());
            userData.get(dbUser.getId()).put("state", "SERVER_CHOICE");
            BotUtil.editMessage(bot,query,"Выберите сервер!",false,buildServerListMenu(serversPage, dbUser));

        }

        if (query.getData().startsWith("raffle-page:")) {
            int page = Integer.parseInt(query.getData().replace("raffle-page:", ""));
            Pageable pageable = PageRequest.of(page, Config.ITEM_ON_PAGE); // Пример создания Pageable
            Page<Raffle> rafflesPage = winnerService.findAllByParticipantId(dbUser.getId(), pageable);

            // Отправка меню пользователю
            BotUtil.editMessage(
                    bot,
                    query,
                    LocalizationService.getString("start.raffleChoice", dbUser.getLang()),
                    false,
                    buildUserWinningsMenu(rafflesPage, dbUser)
            );
        }
        if (query.getData().equals("reward1")) {
            userData.putIfAbsent(dbUser.getId(), new HashMap<>());
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
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
            // Устанавливаем состояние в "SERVER_CHOICE" после отправки списка серверов
            userData.get(dbUser.getId()).put("state", "SERVER_CHOICE");
        }


        if (query.getData().startsWith("server-name:") && userData.get(dbUser.getId()).get("state").equals("SERVER_CHOICE") ) {
            String serverName = query.getData().replace("server-name:", "");
            userData.get(dbUser.getId()).put("serverName", serverName);
            BotUtil.editMessage(
                    bot,
                    (Message) query.getMessage(),
                    LocalizationService.getString("start.enterCharName"),
                    true,
                    null);
            userData.get(dbUser.getId()).put("state", "ENTER_CHAR_NAME");
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
        }
        return false;
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException, SQLException {
        DbUser dbUser = dbUserService.registerUser(message.getFrom());
        if (userData.get(dbUser.getId()).get("state").equals("ENTER_CHAR_NAME")) {
            String charName = message.getText();
            if (charName == null || charName.isEmpty()) {
                BotUtil.editMessage(bot, message, LocalizationService.getString("start.emptyField"), true, null);
                resetState(dbUser.getId());
                return true;
            }
            Manager manager = null;


            Config.switchServer(userData.get(dbUser.getId()).get("serverName"));
            Config.ServerConfig currentServerConfig = Config.getCurrentServerConfig();
            switch (currentServerConfig.serverType) {
                case "Lucera2":
                    manager = new Lucera2DbManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
                    break;
                case "Pain":
                    manager = new PainDbManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
                    break;
                case "L2JEternity":
                    manager = new EternityManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
                    break;
                default:
                    log.error("Неизвестный тип сервера: " + currentServerConfig.serverType);
            }

            if (Objects.requireNonNull(manager).getObjectIdByCharName(charName) == 0) {
                BotUtil.sendMessage(BotApplication.telegramBot, message, LocalizationService.getString("start.incorrectCharName"), false, false, null);
                return true;
            }
            List<Prize> prizeList = new ArrayList<>();

            for (Prize prize : prizeList) {
                if (prize.getType().equals(PrizeType.MONEY)) {
                    BotUtil.sendMessage(BotApplication.telegramBot, message, String.format(LocalizationService.getString("start.moneyCongratulation"), prize.getCount(), prize.getItemName()), false, false, null);
                } else {
                    BotUtil.sendMessage(BotApplication.telegramBot, message, String.format(LocalizationService.getString("start.itemCongratulation"), prize.getCount(), prize.getItemName()), false, false, null);
                    manager.addItem(manager.getObjectIdByCharName(charName), prize.getItemId(), prize.getCount());
                }
            }

            resetState(dbUser.getId());
            return true;
        }
        return false;
    }

    private void resetState(Long userId) {
        userData.remove(userId);
        log.info("Контекст очищен voteMenu");
    }
}
