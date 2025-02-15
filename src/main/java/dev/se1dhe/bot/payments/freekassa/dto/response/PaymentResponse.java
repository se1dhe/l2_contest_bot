package dev.se1dhe.bot.payments.freekassa.dto.response;

import lombok.Data;

@Data
public class PaymentResponse {
    private String type; // "success" в случае успеха
    private int orderId; // Номер заказа Freekassa
    private String orderHash; // Хэш заказа Freekassa
    private String location; // Ссылка на оплату
    private String message; // Сообщение в случае ошибки
}