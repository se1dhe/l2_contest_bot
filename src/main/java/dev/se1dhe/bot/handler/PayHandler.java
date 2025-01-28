package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICommandHandler;
import dev.se1dhe.core.util.BotUtil;
import dev.se1dhe.bot.payments.InitPaymentRequest;
import dev.se1dhe.bot.payments.InitPaymentResponse;
import dev.se1dhe.bot.payments.PaymentsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Log4j2
public class PayHandler implements ICommandHandler {


    private final DBUserService dbUserService;
    private final PaymentsService paymentsService;

    public PayHandler(DBUserService dbUserService, PaymentsService paymentsService) {
        this.dbUserService = dbUserService;
        this.paymentsService = paymentsService;
    }


    @Override
    public String getCommand() {
        return "/pay";
    }

    @Override
    public String getUsage() {
        return "/pay <сумма>";
    }

    @Override
    public String getDescription() {
        return "Пополнить баланс";
    }

    @Override
    public void onCommandMessage(AbstractTelegramBot bot, Update update, Message message, List<String> args) throws TelegramApiException {
        DbUser dbUser = dbUserService.findUserById(message.getFrom().getId());

        if (dbUser == null) {
            BotUtil.sendMessage(bot, message, "Пожалуйста, сначала выполните команду /start", false, false, null);
            return;
        }

        if (args.isEmpty()) {
            BotUtil.sendMessage(bot, message, "Пожалуйста, укажите сумму пополнения. Пример: /pay 100", false, false, null);
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args.get(0));
        } catch (NumberFormatException e) {
            BotUtil.sendMessage(bot, message, "Неверный формат суммы.", false, false, null);
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            BotUtil.sendMessage(bot, message, "Сумма должна быть больше нуля.", false, false, null);
            return;
        }

        InitPaymentRequest paymentRequest = new InitPaymentRequest();
        paymentRequest.setProject(paymentsService.getProjectId());
        paymentRequest.setSum(amount);
        paymentRequest.setCurrency("RUB"); // Валюта
        paymentRequest.setInnerID(String.valueOf(dbUser.getId())); // ID пользователя в вашей системе
        paymentRequest.setEmail("user@example.com"); // Замените на реальный email пользователя
        paymentRequest.setComment("Пополнение баланса");

        try {
            InitPaymentResponse response = paymentsService.initPayment(paymentRequest);
            if ("OK".equals(response.getStatus())) {
                String paymentUrl = response.getResult();
                log.info("Ссылка на оплату: {}", paymentUrl);
                BotUtil.sendMessage(bot, message, "Для пополнения баланса на сумму " + amount + " RUB перейдите по ссылке: " + paymentUrl, false, true, null);
            } else {
                log.error("Ошибка при создании платежа: {}", response.getResult());
                BotUtil.sendMessage(bot, message, "Ошибка при создании платежа: " + response.getResult(), false, true, null);
            }
        } catch (RuntimeException e) { // Обрабатываем RuntimeException
            log.error("Ошибка при обращении к API PrimePayments", e);
            BotUtil.sendMessage(bot, message, "Произошла ошибка при обращении к платежной системе: " + e.getMessage(), false, true, null);
        }
    }
}