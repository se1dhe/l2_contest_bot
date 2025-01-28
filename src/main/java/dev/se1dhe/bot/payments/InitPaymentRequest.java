package dev.se1dhe.bot.payments;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitPaymentRequest {
    private long project;
    private BigDecimal sum;
    private String currency;
    private String innerID;
    private String email;
    private String comment;
    private Boolean needFailNotice;
    private String lang;
    private Integer payWay;
    private Boolean strictPayWay;
    private Boolean blockPayWay;
    private Boolean directPay;
}