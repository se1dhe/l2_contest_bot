package dev.se1dhe.bot.controller;

import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.payments.freekassa.FreeKassaService;
import dev.se1dhe.bot.payments.freekassa.dto.Notification;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/freekassa")
@Log4j2
public class FreeKassaController {

    private final FreeKassaService freeKassaService;



    @Autowired
    public FreeKassaController(FreeKassaService freeKassaService) {
        this.freeKassaService = freeKassaService;
    }

    @PostMapping("/notification")
    public ResponseEntity<String> handleNotification(@RequestParam Map<String, String> allParams) {
        try {
            log.info("Получен запрос на /api/freekassa/notification");
            log.info("Параметры запроса: {}", allParams);

            // Преобразуем параметры запроса в объект Notification
            Notification notification = new Notification();
            notification.setMERCHANT_ID(Integer.parseInt(allParams.get("MERCHANT_ID")));
            notification.setAMOUNT(new BigDecimal(allParams.get("AMOUNT")));
            notification.setIntid(Integer.parseInt(allParams.get("intid")));
            notification.setMERCHANT_ORDER_ID(allParams.get("MERCHANT_ORDER_ID"));
            notification.setP_EMAIL(allParams.get("P_EMAIL"));
            notification.setP_PHONE(allParams.get("P_PHONE"));
            notification.setCUR_ID(allParams.get("CUR_ID"));
            notification.setSIGN(allParams.get("SIGN"));
            // ... (другие поля, если нужно)

            // Установка дополнительных параметров us_
            Map<String, String> usParams = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("us_")) {
                    usParams.put(entry.getKey(), entry.getValue());
                }
            }
            notification.setUs(usParams);

            String response = freeKassaService.handlePaymentNotification(notification);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ошибка при обработке уведомления от FreeKassa", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ERROR");
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> handleSuccessRedirect(@RequestParam Map<String, String> allParams) {
        log.info("Получен запрос на /api/freekassa/success");
        log.info("Параметры запроса: {}", allParams);

        try {
            String merchantOrderId = allParams.get("MERCHANT_ORDER_ID");
            if (merchantOrderId != null) {
                Long userId = Long.valueOf(merchantOrderId);
                BotUtil.sendHtmlMessageById(BotApplication.telegramBot,userId.toString(), "Оплата прошла успешно", null);
                log.info("Пользователю с ID {} отправлено сообщение об успешной оплате.", userId);
            } else {
                log.warn("Параметр MERCHANT_ORDER_ID не найден в запросе.");
            }
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение об успешной оплате!", e);
        } catch (NumberFormatException e) {
            log.error("Неверный формат MERCHANT_ORDER_ID", e);
        }

        return ResponseEntity.ok("<html><body><h1>Payment Successful!</h1><p>Thank you for your payment.</p></body></html>");
    }


    @GetMapping("/failure")
    public ResponseEntity<String> handleFailureRedirect(@RequestParam Map<String, String> allParams) {
        log.info("Получен запрос на /api/freekassa/failure");
        log.info("Параметры запроса: {}", allParams);

        try {
            String merchantOrderId = allParams.get("MERCHANT_ORDER_ID");
            if (merchantOrderId != null) {
                Long userId = Long.valueOf(merchantOrderId);
                BotUtil.sendHtmlMessageById(BotApplication.telegramBot,userId.toString(), "Оплата не прошла!", null);
                log.info("Пользователю с ID {} отправлено сообщение об отмене оплаты.", userId);
            } else {
                log.warn("Параметр MERCHANT_ORDER_ID не найден в запросе.");
            }
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение об отмене оплаты!", e);
        } catch (NumberFormatException e) {
            log.error("Неверный формат MERCHANT_ORDER_ID", e);
        }

        return ResponseEntity.ok("<html><body><h1>Payment Failed!</h1><p>Please try again later.</p></body></html>");
    }
}