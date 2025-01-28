package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class OrderPayedNotification {
    private String action;
    private Long project;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("orderID")})
    private Long orderID;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("date_pay")})
    private Long datePay;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payWay")})
    private Integer payWay;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payed_from")})
    private String payedFrom;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("innerID")})
    private String innerID;
    private BigDecimal sum;
    private String currency;
    private String email;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("webmaster_profit")})
    private BigDecimal webmasterProfit;
    private String sign;

    public String getFormattedDatePay() {
        return Util.formatTimestamp(datePay);
    }
}