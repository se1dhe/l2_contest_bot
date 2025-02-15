package dev.se1dhe.bot.payments.fkwallet.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletBalanceRequest {
    private String walletId;
    private String nonce;
    private String sign;
    private final String action = "balance";
}