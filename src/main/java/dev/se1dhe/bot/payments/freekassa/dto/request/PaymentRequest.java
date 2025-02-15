package dev.se1dhe.bot.payments.freekassa.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PaymentRequest {
    private int m; // ID магазина
    private BigDecimal oa; // Сумма платежа
    private String currency; //Валюта
    private String o; // Номер заказа (в вашей системе)
    private String s; // Подпись
    private String i; // Предлагаемая валюта платежа
    private String phone; // Телефон плательщика
    private String em; // Email плательщика
    private String lang; // Язык интерфейса

    // Дополнительные параметры с префиксом us_
    private Map<String, String> us;
}