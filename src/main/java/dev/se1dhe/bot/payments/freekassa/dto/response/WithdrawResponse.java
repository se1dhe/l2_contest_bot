package dev.se1dhe.bot.payments.freekassa.dto.response;

import lombok.Data;

@Data
public class WithdrawResponse {
    private String type; // "success" в случае успеха
    private Data data;
    private String message; // Сообщение в случае ошибки

    @lombok.Data
    public static class Data {
        private Integer id; // ID созданной выплаты
    }
}