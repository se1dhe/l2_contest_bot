package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.payments.*;
import dev.se1dhe.bot.service.BalanceService;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.LocalizationService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.IMessageHandler;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.KeyboardBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
public class BalanceHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final BalanceService balanceService;
    private final PaymentsService paymentsService;

    // Хранилище для временных данных пользователя
    private final Map<Long, Map<String, String>> userTempData = new HashMap<>();

    @Autowired
    public BalanceHandler(DBUserService dbUserService, BalanceService balanceService, PaymentsService paymentsService) {
        this.dbUserService = dbUserService;
        this.balanceService = balanceService;
        this.paymentsService = paymentsService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        Long userId = query.getFrom().getId();
        DbUser dbUser = dbUserService.findUserById(userId);
        String lang = dbUser.getLang();
        String data = query.getData();
        userTempData.putIfAbsent(userId, new HashMap<>());

        if (data.startsWith("balance")) {
            showBalanceMenu(bot, query, dbUser);
            return true;
        } else if (data.startsWith("deposit")) {
            handleDeposit(bot, query, dbUser);
            return true;
        } else if (data.startsWith("withdraw")) {
            handleWithdraw(bot, query, dbUser);
            return true;
        } else if (data.startsWith("confirm_deposit")) {
            confirmDeposit(bot, query, dbUser);
            return true;
        }
        else if (data.startsWith("back_to_balance")) {
            showBalanceMenu(bot, query, dbUser);
            return true;
        }

        return false;
    }

    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException {
        Long userId = message.getFrom().getId();
        DbUser dbUser = dbUserService.findUserById(userId);

        if (userTempData.containsKey(userId)) {
            String state = userTempData.get(userId).getOrDefault("state", "");

            if (state.equals("awaiting_deposit_amount")) {
                handleDepositAmountInput(bot, message, dbUser);
                return true;
            }
            if (state.equals("awaiting_withdraw_amount")) {
                handleWithdrawAmountInput(bot, message, dbUser);
                return true;
            }
            if (state.equals("awaiting_purse")) {
                handleWithdrawPurseInput(bot, message, dbUser);
                return true;
            }
        }

        return false;
    }

    private void showBalanceMenu(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        String lang = dbUser.getLang();
        BigDecimal balance = balanceService.getBalance(dbUser.getId());
        String message = String.format(LocalizationService.getString("balance.menu", lang), balance);

        InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                .button(LocalizationService.getString("balance.deposit", lang), "deposit")
                .row()
                .button(LocalizationService.getString("balance.withdraw", lang), "withdraw")
                .row()
                .button(LocalizationService.getString("mainMenu.button", lang), "start-menu")
                .row()
                .build();

        BotUtil.editMessage(bot, query, message, false, keyboard);
    }

    private void handleDeposit(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        userTempData.get(dbUser.getId()).put("state", "awaiting_deposit_amount");
        BotUtil.editMessage(bot, query, LocalizationService.getString("deposit.enter_amount", dbUser.getLang()), false, null);
    }

    private void handleDepositAmountInput(AbstractTelegramBot bot, Message message, DbUser dbUser) throws TelegramApiException {
        try {
            BigDecimal amount = new BigDecimal(message.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("deposit.invalid_amount", dbUser.getLang()), false, false, null);
                return;
            }

            userTempData.get(dbUser.getId()).put("deposit_amount", amount.toString());

            InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                    .button(LocalizationService.getString("deposit.confirm", dbUser.getLang()), "confirm_deposit")
                    .row()
                    .button(LocalizationService.getString("registerMenu.back", dbUser.getLang()), "back_to_balance")
                    .build();

            BotUtil.sendMessage(bot, message, String.format(LocalizationService.getString("deposit.confirm_message", dbUser.getLang()), amount), false, false, keyboard);
        } catch (NumberFormatException e) {
            BotUtil.sendMessage(bot, message, LocalizationService.getString("deposit.invalid_amount", dbUser.getLang()), false, false, null);
        }
    }

    private void confirmDeposit(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        String amountString = userTempData.get(dbUser.getId()).get("deposit_amount");
        BigDecimal amount = new BigDecimal(amountString);
        // Здесь вызов метода из PaymentsService для создания ссылки на оплату
        InitPaymentRequest paymentRequest = new InitPaymentRequest();
        paymentRequest.setProject(paymentsService.getProjectId());
        paymentRequest.setSum(amount);
        paymentRequest.setCurrency("RUB");
        paymentRequest.setInnerID(String.valueOf(dbUser.getId()));
        paymentRequest.setEmail("user@example.com"); // Замените на реальный email пользователя
        paymentRequest.setComment("Пополнение баланса");

        try {
            InitPaymentResponse response = paymentsService.initPayment(paymentRequest);
            if ("OK".equals(response.getStatus())) {
                String paymentUrl = response.getResult();
                log.info("Ссылка на оплату: {}", paymentUrl);

                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                        .buttonUrl(LocalizationService.getString("deposit.pay", dbUser.getLang()), paymentUrl)
                        .row()
                        .button(LocalizationService.getString("registerMenu.back", dbUser.getLang()), "back_to_balance")
                        .build();

                BotUtil.editMessage(bot, query, String.format(LocalizationService.getString("deposit.payment_link", dbUser.getLang()), paymentUrl), false, keyboard);
            } else {
                log.error("Ошибка при создании платежа: {}", response.getResult());
                BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("deposit.error", dbUser.getLang()), true);
            }
        } catch (Exception e) {
            log.error("Ошибка при обращении к API PrimePayments", e);
            BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("deposit.error", dbUser.getLang()), true);
        } finally {
            userTempData.get(dbUser.getId()).remove("deposit_amount");
            userTempData.get(dbUser.getId()).put("state", "idle");
        }
    }

    private void handleWithdraw(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        userTempData.get(dbUser.getId()).put("state", "awaiting_withdraw_amount");
        BotUtil.editMessage(bot, query, LocalizationService.getString("withdraw.enter_amount", dbUser.getLang()), false, null);
    }

    private void handleWithdrawAmountInput(AbstractTelegramBot bot, Message message, DbUser dbUser) throws TelegramApiException {
        try {
            BigDecimal amount = new BigDecimal(message.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_amount", dbUser.getLang()), false, false, null);
                return;
            }

            if (balanceService.getBalance(dbUser.getId()).compareTo(amount) < 0) {
                // Добавляем клавиатуру с кнопкой "Главное меню"
                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                        .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                        .build();

                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.insufficient_funds", dbUser.getLang()), false, false, keyboard);
                return;
            }

            userTempData.get(dbUser.getId()).put("withdraw_amount", amount.toString());
            userTempData.get(dbUser.getId()).put("state", "awaiting_purse");

            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.enter_purse", dbUser.getLang()), false, false, null);
        } catch (NumberFormatException e) {
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_amount", dbUser.getLang()), false, false, null);
        }
    }

    private void handleWithdrawPurseInput(AbstractTelegramBot bot, Message message, DbUser dbUser) throws TelegramApiException {
        String purse = message.getText();
        String withdrawOption = userTempData.get(dbUser.getId()).get("withdraw_option");
        log.info("Пользователь {} ввел кошелек: {}", dbUser.getId(), purse);



        BigDecimal amount = new BigDecimal(userTempData.get(dbUser.getId()).get("withdraw_amount"));


        InitPayoutRequest payoutRequest = new InitPayoutRequest();
        payoutRequest.setProject(paymentsService.getProjectId());
        payoutRequest.setSum(amount);
        payoutRequest.setCurrency("RUB");
        payoutRequest.setPayWay(4);
        payoutRequest.setEmail("user@example.com"); // TODO: Замените на реальный email пользователя
        payoutRequest.setPurse(purse);

        InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                .build();

        try {
            InitPayoutResponse response = paymentsService.initPayout(payoutRequest);
            if ("OK".equals(response.getStatus())) {
                balanceService.withdraw(dbUser.getId(), amount);
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.success", dbUser.getLang()), false, false, keyboard);
            } else {
                log.error("Ошибка при создании запроса на вывод: {}", response.getResult());
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.error", dbUser.getLang()), false, false, keyboard);
            }
        } catch (Exception e) {
            log.error("Ошибка при обращении к API PrimePayments", e);
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.error", dbUser.getLang()), false, false, keyboard);
        } finally {
            userTempData.get(dbUser.getId()).remove("withdraw_amount");
            userTempData.get(dbUser.getId()).remove("purse");
            userTempData.get(dbUser.getId()).remove("withdraw_option");
            userTempData.get(dbUser.getId()).put("state", "idle");
        }
    }
}