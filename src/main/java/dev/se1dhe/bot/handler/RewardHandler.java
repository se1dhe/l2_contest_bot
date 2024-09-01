

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
import dev.se1dhe.core.handlers.inline.*;
import dev.se1dhe.core.handlers.inline.events.IInlineCallbackEvent;
import dev.se1dhe.core.handlers.inline.events.IInlineMessageEvent;
import dev.se1dhe.core.handlers.inline.events.InlineCallbackEvent;
import dev.se1dhe.core.handlers.inline.layout.InlineFixedButtonsPerRowLayout;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@Log4j2
public class RewardHandler extends AbstractInlineHandler {
    private static String WINNER_ID_FIELD;
    private static String RAFFLE_NAME_BONUS_FIELD;
    private static final String RAFFLE_NAME_FIELD = "raffle_name";
    private final DBUserService dbUserService;
    private final RaffleService raffleService;
    private final WinnerService winnerService;
    private final RaffleBonusService raffleBonusService;

    private final AtomicBoolean createdAdmin = new AtomicBoolean();

    public RewardHandler(DBUserService dbUserService, RaffleService raffleService, WinnerService winnerService, RaffleBonusService raffleBonusService) {
        this.dbUserService = dbUserService;
        this.raffleService = raffleService;
        this.winnerService = winnerService;
        this.raffleBonusService = raffleBonusService;
    }

    @Override
    public String getCommand() {
        return "/test";
    }

    @Override
    public String getUsage() {
        return "/test";
    }

    @Override
    public String getDescription() {
        return "/test";
    }

    @Override
    public void registerMenu(InlineContext ctx, InlineMenuBuilder builder) {
        builder
                .name(LocalizationService.getString("start.welcomeMessage"))
                .button(new InlineButtonBuilder(ctx)
                        .name(LocalizationService.getString("start.getPrize"))
                        .row(0)
                        .onQueryCallback(this::handleGetPrize)
                        .build())
                .button(new InlineButtonBuilder(ctx)
                        .name(LocalizationService.getString("start.getBonus"))
                        .row(0)
                        .onQueryCallback(this::handleGetBonus)
                        .build())
                .button(new InlineButtonBuilder(ctx)
                        .name(LocalizationService.getString("contact.button"))
                        .row(1)
                        .onQueryCallback(this::contactHandler)
                        .build())

                .button(defaultClose(ctx))
                .build();
    }

    private boolean contactHandler(InlineCallbackEvent event) throws TelegramApiException {
        final InlineUserData userData = event.getContext().getUserData(event.getQuery().getFrom().getId());
        if (userData.getState() == 0) {
            BotUtil.editMessage(BotApplication.telegramBot, (Message) event.getQuery().getMessage(), LocalizationService.getString("contact.message"), false, null);
            return true;
        }
        return false;
    }

    private boolean handleGetPrize(InlineCallbackEvent event) throws TelegramApiException {

        DbUser dbUser = dbUserService.registerUser(event.getQuery().getFrom());

        List<Prize> prizeList = new ArrayList<>();
        final IInlineCallbackEvent onQueryCallback = evt ->
        {
            final InlineUserData userData = evt.getContext().getUserData(evt.getQuery().getFrom().getId());
            if (userData.getState() == 0) {
                userData.setState(1);
                userData.getParams().put(RAFFLE_NAME_FIELD, userData.getActiveButton().getName());
                BotUtil.editMessage(BotApplication.telegramBot, (Message) evt.getQuery().getMessage(), LocalizationService.getString("start.enterCharName"), false, null);
                return true;
            }
            return false;
        };

        final IInlineMessageEvent onInputMessage = evt -> {
            Manager manager = null;

            try {
                // Пример переключения на конкретный сервер
                Config.switchServer("Server One"); // Имя сервера из файла конфигурации

                // Получаем текущую конфигурацию сервера
                Config.ServerConfig currentServerConfig = Config.getCurrentServerConfig();

                // Создаем соответствующий менеджер на основе типа сервера
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

                // Работа с менеджером
                if (manager != null) {
                    manager.addItem(12345, 678, 10); // Пример вызова метода
                    manager.close();
                }

            } catch (SQLException e) {
                log.error("Ошибка при работе с базой данных: " + e.getMessage());
            } finally {
                if (manager != null) {
                    try {
                        manager.close();
                    } catch (SQLException e) {
                        log.error("Ошибка при закрытии соединения с базой данных: " + e.getMessage());
                    }
                }
            }
            return false;
        };

        List<Raffle> raffleList = raffleService.getAllRaffles();
        final InlineUserData userData = event.getContext().getUserData(event.getQuery().getFrom().getId());
        final InlineMenuBuilder usersBuilder = new InlineMenuBuilder(event.getContext(), userData.getActiveMenu());
        usersBuilder.name(LocalizationService.getString("start.raffleChoice"));
        for (Raffle raffle : raffleList) {
            for (Winner winner : winnerService.findAllByRaffleIdAndParticipantId(raffle.getId(),event.getQuery().getFrom().getId())) {
                if (winner.getPrize() != null && !winner.isGetPrize()) {
                    usersBuilder.button(new InlineButtonBuilder(event.getContext())
                            .name("❇ "+raffle.getName())
                            .forceOnNewRow()
                            .onQueryCallback(onQueryCallback)
                            .onInputMessage(onInputMessage)
                            .build());
                    prizeList.add(winner.getPrize());
                    WINNER_ID_FIELD = String.valueOf(winner.getId());
                }

            }
        }
        usersBuilder.button(defaultBack(event.getContext()));

        final InlineMenu usersMenu = usersBuilder.build();
        userData.editCurrentMenu(BotApplication.telegramBot, (Message) event.getQuery().getMessage(), new InlineFixedButtonsPerRowLayout(3), usersMenu);
        return true;
    }


    private boolean handleGetBonus(InlineCallbackEvent event) throws TelegramApiException {
        DbUser dbUser = dbUserService.registerUser(event.getQuery().getFrom());
        final IInlineCallbackEvent onQueryCallback = evt -> {
            final InlineUserData userData = evt.getContext().getUserData(evt.getQuery().getFrom().getId());
            if (userData.getState() == 0) {
                userData.setState(1);
                userData.getParams().put(RAFFLE_NAME_FIELD, userData.getActiveButton().getName());
                BotUtil.editMessage(BotApplication.telegramBot, (Message) evt.getQuery().getMessage(), LocalizationService.getString("start.enterCharName"), false, null);
                return true;
            }
            return false;
        };

        final IInlineMessageEvent onInputMessage = evt -> {
            Manager manager = null;

            // Получаем текущую серверную конфигурацию
            Config.ServerConfig currentServerConfig = Config.getCurrentServerConfig();
            if (currentServerConfig == null) {
                BotUtil.sendMessage(BotApplication.telegramBot, evt.getMessage(), LocalizationService.getString("start.serverConfigMissing"), false, false, null);
                return true;
            }

            // Инициализируем менеджера в зависимости от текущей конфигурации сервера
            if ("pain".equalsIgnoreCase(Config.SERVER_COMMAND_NAME)) {
                manager = new PainDbManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
            } else if ("lucera2".equalsIgnoreCase(Config.SERVER_COMMAND_NAME)) {
                manager = new Lucera2DbManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
            } else if ("l2jEternity".equalsIgnoreCase(Config.SERVER_COMMAND_NAME)) {
                manager = new EternityManager(currentServerConfig.url, currentServerConfig.username, currentServerConfig.password);
            }

            final InlineUserData userData = evt.getContext().getUserData(evt.getMessage().getFrom().getId());
            if (userData.getState() == 1) {
                final String charName = evt.getMessage().getText();
                if ((charName == null) || charName.isEmpty()) {
                    BotUtil.sendMessage(BotApplication.telegramBot, evt.getMessage(), LocalizationService.getString("start.emptyField"), false, false, null);
                    return true;
                }

                assert manager != null;
                if (manager.getObjectIdByCharName(charName) == 0) {
                    BotUtil.sendMessage(BotApplication.telegramBot, evt.getMessage(), LocalizationService.getString("start.incorrectCharName"), false, false, null);
                    return true;
                }

                if (!Config.DAILY_PARTICIPANT_BONUS) {
                    BotUtil.sendMessage(BotApplication.telegramBot, evt.getMessage(), LocalizationService.getString("start.bonusDisable"), false, false, null);
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

                BotUtil.sendMessage(BotApplication.telegramBot, evt.getMessage(), s, false, false, null);
                Raffle raffle = raffleService.getRaffleById(Long.valueOf(RAFFLE_NAME_BONUS_FIELD));
                raffle.getParticipant().remove(dbUser);
                raffleBonusService.create(raffle.getId(), dbUser.getId());
                evt.getContext().clear(evt.getMessage().getFrom().getId());
                return true;
            }

            evt.getContext().clear(evt.getMessage().getFrom().getId());
            return false;
        };

        List<Raffle> raffleList = raffleService.getAllRaffles();
        final InlineUserData userData = event.getContext().getUserData(event.getQuery().getFrom().getId());
        final InlineMenuBuilder usersBuilder = new InlineMenuBuilder(event.getContext(), userData.getActiveMenu());
        usersBuilder.name(LocalizationService.getString("start.raffleChoice"));

        Set<Long> addedRaffles = new HashSet<>();

        for (Raffle raffle : raffleList) {
            if (raffleBonusService.findRaffleBonusByRaffleIdAndDbUserId(raffle.getId(), event.getQuery().getFrom().getId()) == null) {
                for (DbUser participant : raffle.getParticipant()) {
                    if (participant.getId().equals(event.getQuery().getFrom().getId())) {
                        for (Winner winner : winnerService.findByRaffle(raffle)) {
                            if (!Objects.equals(winner.getParticipant().getId(), participant.getId())
                                    && addedRaffles.add(raffle.getId())) {
                                usersBuilder.button(new InlineButtonBuilder(event.getContext())
                                        .name("❇ " + raffle.getName())
                                        .forceOnNewRow()
                                        .onQueryCallback(onQueryCallback)
                                        .onInputMessage(onInputMessage)
                                        .build());
                                RAFFLE_NAME_BONUS_FIELD = String.valueOf(raffle.getId());
                                break;
                            }
                        }
                    }
                }
            }
        }

        usersBuilder.button(defaultBack(event.getContext()));

        final InlineMenu usersMenu = usersBuilder.build();
        userData.editCurrentMenu(BotApplication.telegramBot, (Message) event.getQuery().getMessage(), new InlineFixedButtonsPerRowLayout(3), usersMenu);
        return true;
    }

}
