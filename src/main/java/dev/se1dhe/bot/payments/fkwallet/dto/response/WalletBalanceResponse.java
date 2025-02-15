package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class WalletBalanceResponse {
    private String status;
    private Map<String, String> data;
    private String message;


    public String getBalance() {
        return data != null && data.containsKey("USDT") ? data.get("USDT") : null;
    }
}