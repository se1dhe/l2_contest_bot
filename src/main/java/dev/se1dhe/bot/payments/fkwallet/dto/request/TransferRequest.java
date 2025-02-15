package dev.se1dhe.bot.payments.fkwallet.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferRequest {
    private String walletId;
    private String purse;
    private BigDecimal amount;
    private String sign;
    private final String action = "transfer";
    private String desc; // Описание перевода, может быть null
    private String currency; // Идентификатор валюты, может быть null
}