package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class OrderCancelNotification {
    private String action;
    private Long project;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("orderID")})
    private Long orderID;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payed_from")})
    private String payedFrom;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("innerID")})
    private String innerID;
    private BigDecimal sum;
    private String currency;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("date_pay")})
    private Long datePay;
    private String sign;

    public String getFormattedDatePay() {
        return Util.formatTimestamp(datePay);
    }
}