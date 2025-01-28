package dev.se1dhe.bot.payments;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitPayoutRequest {
    private long project;
    private BigDecimal sum;
    private String currency;
    private int payWay;
    private String email;
    private String purse;
    private String cardholder;
    private String comment;
    private String sbpId;
    private String needUnique;
}