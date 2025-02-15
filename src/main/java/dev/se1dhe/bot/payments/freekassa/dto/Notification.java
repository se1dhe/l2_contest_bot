package dev.se1dhe.bot.payments.freekassa.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class Notification {
    private int MERCHANT_ID;
    private BigDecimal AMOUNT;
    private int intid;
    private String MERCHANT_ORDER_ID;
    private String P_EMAIL;
    private String P_PHONE;
    private String CUR_ID;
    private String SIGN;
    private String payer_account;
    private BigDecimal commission;
    // ... другие параметры, которые вам нужны
    private Map<String, String> us; // Дополнительные параметры
}