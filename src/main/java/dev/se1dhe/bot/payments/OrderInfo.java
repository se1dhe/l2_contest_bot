package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class OrderInfo {
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("order_id")})
    private Long orderId;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("date_add")})
    private Long dateAdd;
    private BigDecimal sum;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payWay")})
    private Integer payWay;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payed_from")})
    private String payedFrom;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("innerID")})
    private String innerID;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("pay_status")})
    private Integer payStatus;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("webmaster_profit")})
    private BigDecimal webmasterProfit;

    public String getFormattedDateAdd() {
        return Util.formatTimestamp(dateAdd);
    }
}