package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class TransferResponse {
    private String status;
    private String message;
    private Map<String, Object> data;
}