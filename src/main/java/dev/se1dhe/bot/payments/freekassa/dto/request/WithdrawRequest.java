package dev.se1dhe.bot.payments.freekassa.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WithdrawRequest {
    private int shopId;
    private String nonce;
    private String signature;
    private int i; // Изменено с paymentId на i
    private String account;
    private BigDecimal amount;
    private String currency;
    private String desc;
    // другие необходимые параметры, если требуются
}