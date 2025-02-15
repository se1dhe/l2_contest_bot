package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;

@Data
public class CurrencyData {
    private int id;
    private String code;
    private String name;
    private String minimum;
}