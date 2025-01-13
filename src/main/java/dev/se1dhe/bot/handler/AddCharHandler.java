package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.GameUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.GameUserService;
import dev.se1dhe.bot.service.LocalizationService;
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
import utils.KeyboardBuilder;
import utils.menu.MainMenu;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static utils.menu.MainMenu.*;

@Service
@Log4j2
public class AddCharHandler implements ICallbackQueryHandler, IMessageHandler {
    private final DBUserService dbUserService;
    private final GameUserService gameUserService;
    private final Map<Long, Map<String, String>> userData = new HashMap<>();


    public AddCharHandler(DBUserService dbUserService, GameUserService gameUserService) {
        this.dbUserService = dbUserService;
        this.gameUserService = gameUserService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(query.getFrom());

        userData.putIfAbsent(dbUser.getId(), new HashMap<>());
        userData.get(dbUser.getId()).putIfAbsent("state", "INIT");
        if (query.getData().startsWith("add-character")) {
            Pageable pageable = PageRequest.of(0, Config.ITEM_ON_PAGE);
            List<Config.ServerConfig> servers = Config.getAvailableServers();
            int start = Math.min((int) pageable.getOffset(), servers.size());
            int end = Math.min((start + pageable.getPageSize()), servers.size());
            Page<Config.ServerConfig> serversPage = new PageImpl<>(servers.subList(start, end), pageable, servers.size());
            userData.get(dbUser.getId()).put("state", "ADD_CHAR_SERVER_CHOICE");
            BotUtil.editMessage(bot, query, "Выберите сервер!", false, buildServerListMenu(serversPage, dbUser));
            return true;
        }
        if (query.getData().startsWith("del-all-character")) {
            if (gameUserService.findByDbUserId(dbUser.getId()).isEmpty()) {
                BotUtil.editMessage(bot, query, "У Вас нет привязанных персонажей!", false, KeyboardBuilder.inline()
                        .button(LocalizationService.getString("registerMenu.back"),"start-menu")
                        .build());
                return true;
            }
            gameUserService.deleteAllByDbUser(dbUser);
            BotUtil.editMessage(bot, query, "Вы успешно отвязали всех существующих персонажей!", false, KeyboardBuilder.inline()
                            .button(LocalizationService.getString("registerMenu.back"),"start-menu")
                    .build());
            return true;
        }



        if (query.getData().startsWith("server-name:") && userData.get(dbUser.getId()).get("state").equals("ADD_CHAR_SERVER_CHOICE")) {
            String addCharServerName = query.getData().replace("server-name:", "");

            // Проверяем, есть ли уже привязанный персонаж на этом сервере
            Optional<GameUser> existingGameUser = gameUserService.findByDbUserAndServerName(dbUser, addCharServerName);
            if (existingGameUser.isPresent()) {
                BotUtil.editMessage(
                        bot,
                        (Message) query.getMessage(),
                        "У вас уже есть привязанный персонаж на сервере " + addCharServerName + "!",
                        true,
                        KeyboardBuilder.inline().button(LocalizationService.getString("registerMenu.back"),"start-menu").build()
                );
                return true;
            }

            userData.get(dbUser.getId()).put("addCharServerName", addCharServerName);
            BotUtil.editMessage(
                    bot,
                    (Message) query.getMessage(),
                    LocalizationService.getString("addChar.enterCharName"),
                    true,
                    null
            );
            userData.get(dbUser.getId()).put("state", "ADD_CHAR_NAME");
            return true;
        }
        return false;
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException, SQLException {
        DbUser dbUser = dbUserService.registerUser(message.getFrom());
        if (userData.get(dbUser.getId()).get("state").equals("ADD_CHAR_NAME")) {
            String charName = message.getText();

            if (charName == null || charName.isEmpty()) {
                log.warn("Пользователь {} ввел пустое имя персонажа", dbUser.getId());
                BotUtil.editMessage(bot, message, LocalizationService.getString("start.emptyField"), true, null);
                resetState(dbUser.getId());
                return true;
            }

            Manager manager = getManager(userData.get(dbUser.getId()).get("addCharServerName"));
            if (manager == null) {
                log.error("Не удалось создать менеджер для userId={}", dbUser.getId());
                return true;
            }

            if (manager.getObjectIdByCharName(charName) == 0) {
                log.warn("Некорректное имя персонажа {} для userId={}", charName, dbUser.getId());
                BotUtil.sendMessage(BotApplication.telegramBot, message, LocalizationService.getString("start.incorrectCharName"), false, false, null);
                return true;
            }
            GameUser gameUser = gameUserService.createGameUser(message.getFrom().getId(), (long) manager.getObjectIdByCharName(charName),userData.get(dbUser.getId()).get("addCharServerName"));
            BotUtil.sendMessage(BotApplication.telegramBot, message, String.format(LocalizationService.getString("addChar.successMessage"),gameUser.getCode()), false, false, MainMenu.mainMenu(dbUser));
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


    private void resetState(Long userId) {
        userData.remove(userId);
        log.info("Контекст очищен для userId={}", userId);
    }
}
