package dev.se1dhe.bot.handler;

import dev.se1dhe.bot.exception.WithdrawException;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.payments.freekassa.dto.request.PaymentRequest;
import dev.se1dhe.bot.payments.freekassa.FreeKassaService;
import dev.se1dhe.bot.payments.fkwallet.FKWalletService;
import dev.se1dhe.bot.payments.fkwallet.dto.request.WithdrawFkWalletRequest;
import dev.se1dhe.bot.payments.fkwallet.dto.response.CurrenciesResponse;
import dev.se1dhe.bot.payments.fkwallet.dto.response.Currency;
import dev.se1dhe.bot.payments.fkwallet.dto.response.PaymentSystem;
import dev.se1dhe.bot.payments.fkwallet.dto.response.WithdrawFkWalletResponse;
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
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Log4j2
public class BalanceHandler implements ICallbackQueryHandler, IMessageHandler {

    private final DBUserService dbUserService;
    private final BalanceService balanceService;
    private final FreeKassaService freeKassaService;

    private final FKWalletService fkWalletService;

    private final Map<Long, Map<String, String>> userTempData = new HashMap<>();

    @Autowired
    public BalanceHandler(DBUserService dbUserService, BalanceService balanceService, FreeKassaService freeKassaService, FKWalletService fkWalletService) {
        this.dbUserService = dbUserService;
        this.balanceService = balanceService;
        this.freeKassaService = freeKassaService;
        this.fkWalletService = fkWalletService;
    }

    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        Long userId = query.getFrom().getId();
        DbUser dbUser = dbUserService.findUserById(userId);
        String lang = dbUser.getLang();
        String data = query.getData();

        log.info("Received callback query: user={}, data={}", userId, data);

        userTempData.putIfAbsent(userId, new HashMap<>());

        try {
            if (data.startsWith("balance")) {
                showBalanceMenu(bot, query, dbUser);
                return true;
            } else if (data.startsWith("deposit")) {
                handleDeposit(bot, query, dbUser);
                return true;
            }

            else if (data.startsWith("withdraw_option:")) {
                String option = data.replace("withdraw_option:", "").trim();
                log.info("User {} selected withdraw option: {}", userId, option);
                handleWithdrawOption(bot, query, dbUser, option);
                return true;
            }
            else if (data.startsWith("withdraw")) {
                handleWithdraw(bot, query, dbUser);
                return true;
            } else if (data.startsWith("confirm_deposit")) {
                confirmDeposit(bot, query, dbUser);
                return true;
            } else if (data.startsWith("back_to_balance")) {
                showBalanceMenu(bot, query, dbUser);
                return true;
            } else if (data.equals("check_balance")) {
                checkFKWalletBalance(bot, query, dbUser);
                return true;
            } else if (data.equals("show_payment_systems")) {
                showPaymentSystems(bot, query, dbUser);
                return true;
            } else if (data.equals("currencies")) {
                showAvailableCurrencies(bot, query, dbUser);
                return true;
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке CallbackQuery", e);
            BotUtil.sendAnswerCallbackQuery(bot, query, "Произошла ошибка", true);
        }

        return false;
    }



    @Override
    public boolean onMessage(AbstractTelegramBot bot, Update update, Message message) throws TelegramApiException {
        Long userId = message.getFrom().getId();
        DbUser dbUser = dbUserService.findUserById(userId);

        if (userTempData.containsKey(userId)) {
            String state = userTempData.get(userId).getOrDefault("state", "");
            log.info("User {} message received. State: {}", userId, state); // Логируем получение сообщения и состояние
            try {
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
            } catch (Exception e) {
                log.error("Ошибка при обработке Message", e);
                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                        .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                        .build();
                BotUtil.sendMessage(bot, message, "Произошла ошибка", false, false, keyboard);
            }
        }

        return false;
    }

    private void showBalanceMenu(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        String lang = dbUser.getLang();
        BigDecimal balance = balanceService.getBalance(dbUser.getId());
        String message = String.format(LocalizationService.getString("balance.menu", lang), balance);

        try {
            BigDecimal exchangeRate = fkWalletService.getExchangeRate("USDT");

            if (exchangeRate.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal result = balance.divide(exchangeRate, 2, RoundingMode.HALF_UP);
                String fkwalletBalance = result.toString();

                message += "\n" + String.format(LocalizationService.getString("balance.fkwallet", lang), fkwalletBalance);
            } else {
                message += "\n" + LocalizationService.getString("balance.fkwallet.error", lang);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении баланса FKWallet", e);
            message += "\n" + LocalizationService.getString("balance.fkwallet.error", lang);
        }

        InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                .button(LocalizationService.getString("balance.deposit", lang), "deposit")
                .row()
                .button(LocalizationService.getString("balance.withdraw", lang), "withdraw")
                .row()
                .button(LocalizationService.getString("balance.check", lang), "check_balance")
                .row()
                .button(LocalizationService.getString("mainMenu.button", lang), "start-menu")
                .row()
                .build();

        BotUtil.sendMessage(bot, (Message) query.getMessage(), message, false, false, keyboard);
    }


    private void handleDeposit(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        userTempData.get(dbUser.getId()).put("state", "awaiting_deposit_amount");
        BotUtil.sendMessage(bot, (Message) query.getMessage(), LocalizationService.getString("deposit.enter_amount", dbUser.getLang()), false, false, null); //editMessage -> sendMessage
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
        Map<String, String> usParams = new HashMap<>();
        usParams.put("userid", String.valueOf(dbUser.getId()));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .m(freeKassaService.getMerchantId())
                .oa(amount)
                .o(String.valueOf(dbUser.getId()))
                .s(freeKassaService.calculateSign(freeKassaService.getMerchantId(), amount.toPlainString(), "RUB", String.valueOf(dbUser.getId())))
                .currency("RUB")
                .em(dbUser.getUserName())
                .us(usParams)
                .build();

        try {
            String paymentUrl = freeKassaService.createPayment(paymentRequest);
            log.info("Ссылка на оплату: {}", paymentUrl);

            InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                    .buttonUrl(LocalizationService.getString("deposit.pay", dbUser.getLang()), paymentUrl) // Используем URL
                    .row()
                    .button(LocalizationService.getString("registerMenu.back", dbUser.getLang()), "back_to_balance")
                    .build();

            BotUtil.editMessage(bot, query, String.format(LocalizationService.getString("deposit.payment_link", dbUser.getLang()), paymentUrl), false, keyboard);

        } catch (Exception e) {
            log.error("Ошибка при обращении к API FreeKassa", e);
            BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("deposit.error", dbUser.getLang()), true);
        } finally {
            userTempData.get(dbUser.getId()).remove("deposit_amount");
            userTempData.get(dbUser.getId()).put("state", "idle");
        }
    }
    private void handleWithdraw(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        showWithdrawalOptions(bot, query, dbUser);
    }
    private void showWithdrawalOptions(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                .button(LocalizationService.getString("withdraw.option.card", dbUser.getLang()), "withdraw_option:card")
                .row()
                .button(LocalizationService.getString("withdraw.option.trc20", dbUser.getLang()), "withdraw_option:trc20")
                .row()
                .button(LocalizationService.getString("registerMenu.back", dbUser.getLang()), "back_to_balance")
                .build();
        BotUtil.sendMessage(bot, (Message) query.getMessage(), LocalizationService.getString("withdraw.select_option", dbUser.getLang()), false, false, keyboard);
    }

    private void handleWithdrawOption(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser, String option) throws TelegramApiException {
        userTempData.get(dbUser.getId()).put("withdraw_option", option); // Сохраняем выбор
        userTempData.get(dbUser.getId()).put("state", "awaiting_withdraw_amount");
        String messageKey = "withdraw.enter_amount";
        if ("trc20".equals(option)) {
            messageKey = "withdraw.enter_amount_trc20";
        } else if ("card".equals(option)) {
            messageKey = "withdraw.enter_amount";
        }
        BotUtil.editMessage(bot, (Message) query.getMessage(), LocalizationService.getString(messageKey, dbUser.getLang()), false, null);
    }

    private void handleWithdrawAmountInput(AbstractTelegramBot bot, Message message, DbUser dbUser) throws TelegramApiException {
        try {
            BigDecimal amount = new BigDecimal(message.getText());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_amount", dbUser.getLang()), false, false, null);
                return;
            }

            String withdrawOption = userTempData.get(dbUser.getId()).get("withdraw_option");
            BigDecimal amountInRub = amount;

            if ("trc20".equals(withdrawOption)) {
                BigDecimal usdtToRubRate = new BigDecimal(String.valueOf(fkWalletService.getExchangeRate("USDT")));
                amountInRub = amount.multiply(usdtToRubRate);
            }

            if (balanceService.getBalance(dbUser.getId()).compareTo(amountInRub) < 0) {
                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                        .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                        .build();
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.insufficient_funds", dbUser.getLang()), false, false, keyboard);
                return;
            }

            userTempData.get(dbUser.getId()).put("withdraw_amount", amount.toPlainString()); // Сохраняем введенную пользователем сумму (USDT или RUB)
            if ("trc20".equals(withdrawOption)) {
                userTempData.get(dbUser.getId()).put("withdraw_amount_rub", amountInRub.toPlainString()); // Сохраняем в рублях
            }

            userTempData.get(dbUser.getId()).put("state", "awaiting_purse");

            if ("trc20".equals(withdrawOption)) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.enter_purse", dbUser.getLang()), false, false, null);
            } else if ("card".equals(withdrawOption)) {
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.enter_card", dbUser.getLang()), false, false, null);
            }

        } catch (NumberFormatException e) {
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_amount", dbUser.getLang()), false, false, null);
        }  catch (Exception e) {
            log.error("Ошибка при получении курса USDT/RUB", e);
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.error.rate", dbUser.getLang()), false, false, null);
        }
    }
    private void handleWithdrawPurseInput(AbstractTelegramBot bot, Message message, DbUser dbUser) throws TelegramApiException {
        String purse = message.getText();
        String withdrawOption = userTempData.get(dbUser.getId()).get("withdraw_option");

        if ("trc20".equals(withdrawOption) && !isValidTRC20(purse)) {
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_purse_trc20", dbUser.getLang()), false, false, null);
            return;
        } else if ("card".equals(withdrawOption) && !isValidCardNumber(purse)) {
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.invalid_purse_card", dbUser.getLang()), false, false, null); //  withdraw.invalid_card
            return;
        }

        BigDecimal amount;
        if ("trc20".equals(withdrawOption)) {
            amount = new BigDecimal(userTempData.get(dbUser.getId()).get("withdraw_amount"));
        } else {
            amount = new BigDecimal(userTempData.get(dbUser.getId()).get("withdraw_amount"));
        }

        WithdrawFkWalletRequest withdrawRequest = WithdrawFkWalletRequest.builder()
                .amount(amount)
                .currency("trc20".equals(withdrawOption) ? 11 : 1)
                .paymentSystemId("trc20".equals(withdrawOption) ? 4: 6)
                .feeFromBalance(0)
                .account(purse)
                .description("Вывод средств: " + dbUser.getId())
                .orderId(Integer.valueOf(String.valueOf(dbUser.getId())))
                .idempotenceKey(UUID.randomUUID().toString())
                .build();

        InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                .build();

        try {
            WithdrawFkWalletResponse response =  fkWalletService.createWithdrawal(withdrawRequest);
            if (response != null && response.getStatus().equalsIgnoreCase("success")) {
                balanceService.withdraw(dbUser.getId(), "trc20".equals(withdrawOption) ?
                        new BigDecimal(userTempData.get(dbUser.getId()).get("withdraw_amount_rub")) : amount);
                BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.success", dbUser.getLang()), false, false, keyboard);
            } else {
                String errorMessage = (response != null && response.getMessage() != null) ?
                        response.getMessage() :
                        LocalizationService.getString("withdraw.error", dbUser.getLang());
                log.error("Ошибка при создании запроса на вывод: {}", errorMessage);
                BotUtil.sendMessage(bot, message, errorMessage, false, false, keyboard);
            }
        } catch (WithdrawException e) {
            log.error("Ошибка при выводе средств", e);
            BotUtil.sendMessage(bot, message, e.getMessage(), false, false, keyboard);

        }    catch (Exception e) {
            log.error("Ошибка при обращении к API FKWallet", e);
            BotUtil.sendMessage(bot, message, LocalizationService.getString("withdraw.error", dbUser.getLang()), false, false, keyboard);
        } finally {
            userTempData.get(dbUser.getId()).remove("withdraw_amount");
            userTempData.get(dbUser.getId()).remove("withdraw_amount_usdt");
            userTempData.get(dbUser.getId()).remove("withdraw_amount_rub");
            userTempData.get(dbUser.getId()).remove("purse");
            userTempData.get(dbUser.getId()).remove("withdraw_option");
            userTempData.get(dbUser.getId()).put("state", "idle");
        }
    }


    private void checkFKWalletBalance(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        try {
            String balance = fkWalletService.getWalletBalance();
            BotUtil.sendAnswerCallbackQuery(bot, query, String.format(LocalizationService.getString("balance.fkwallet", dbUser.getLang()), balance), false);
        } catch (Exception e) {
            log.error("Ошибка при получении баланса FKWallet", e);
            BotUtil.sendAnswerCallbackQuery(bot, query, LocalizationService.getString("balance.fkwallet.error", dbUser.getLang()), true);
        }
    }
    private void showAvailableCurrencies(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        try {
            CurrenciesResponse currenciesResponse = fkWalletService.getCurrencies();
            if (currenciesResponse != null && "success".equals(currenciesResponse.getStatus())) {
                StringBuilder message = new StringBuilder(LocalizationService.getString("currencies.list", dbUser.getLang()));
                for (dev.se1dhe.bot.payments.fkwallet.dto.response.Currency currency : currenciesResponse.getData()) {
                    message.append(String.format("\n- %s (ID: %d, Курс: %s)", currency.getCode(), currency.getId(), currency.getCourse()));
                }

                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()//добавляем кнопку
                        .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                        .build();
                BotUtil.sendMessage(bot, (Message) query.getMessage(), message.toString(), false, false, keyboard);

            } else {
                log.error("Ошибка при получении списка валют: {}", currenciesResponse != null ? currenciesResponse.getData() : "null response");
                InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()//добавляем кнопку
                        .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                        .build();
                BotUtil.sendMessage(bot,  (Message) query.getMessage(), LocalizationService.getString("currencies.error", dbUser.getLang()), false, false, keyboard);

            }
        } catch (Exception e) {
            log.error("Ошибка при обращении к API FKWallet", e);
            InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()//добавляем кнопку
                    .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                    .build();
            BotUtil.sendMessage(bot, (Message) query.getMessage(), LocalizationService.getString("currencies.error", dbUser.getLang()), false, false, keyboard);
        }
    }
    private void showPaymentSystems(AbstractTelegramBot bot, CallbackQuery query, DbUser dbUser) throws TelegramApiException {
        try {
            List<PaymentSystem> paymentSystems = fkWalletService.getPaymentSystems();
            String message = LocalizationService.getString("payment_systems.list", dbUser.getLang()) + "\n\n";
            for (PaymentSystem ps : paymentSystems) {
                message += String.format("%s (ID: %s)\n", ps.getName(), ps.getId());
                if (ps.getAvailableCurrencies() != null && !ps.getAvailableCurrencies().isEmpty()) {
                    message += "    " + LocalizationService.getString("payment_systems.currencies", dbUser.getLang()) + ": " + String.join(", ", ps.getAvailableCurrencies()) + "\n";
                }
                message += "\n";
            }

            InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                    .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                    .build();
            BotUtil.sendMessage(bot, (Message) query.getMessage(), message, false, false, keyboard);


        } catch (Exception e) {
            log.error("Ошибка при получении списка платежных систем", e);
            InlineKeyboardMarkup keyboard = KeyboardBuilder.inline()
                    .button(LocalizationService.getString("mainMenu.button", dbUser.getLang()), "start-menu")
                    .build();
            BotUtil.sendMessage(bot, (Message) query.getMessage(), LocalizationService.getString("payment_systems.error", dbUser.getLang()), false, false, keyboard);

        }
    }

    private boolean isValidTRC20(String purse) {
        if (!purse.startsWith("T") || purse.length() != 34) {
            log.info("Неверный формат кошелька TRC20: {}", purse);
            return false;
        }

        for (int i = 0; i < purse.length(); i++) {
            char c = purse.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                log.info("Неверный формат кошелька TRC20: {}", purse);
                return false;
            }
        }
        log.info("Кошелек TRC20 прошел валидацию: {}", purse);
        return true;
    }


    private boolean isValidCardNumber(String cardNumber) {
        cardNumber = cardNumber.replaceAll("\\s+", "");

        if (!Pattern.matches("^\\d{13,19}$", cardNumber)) {
            log.info("Неверный формат номера карты: {}", cardNumber);
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }

        if (sum % 10 == 0) {
            log.info("Номер карты прошел валидацию по алгоритму Луна: {}", cardNumber);
            return true;
        } else {
            log.info("Номер карты не прошел проверку по алгоритму Луна: {}", cardNumber);
            return false;
        }
    }
}