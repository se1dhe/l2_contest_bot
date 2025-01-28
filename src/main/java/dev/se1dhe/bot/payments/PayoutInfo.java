package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class PayoutInfo {
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payout_id")})
    private Long payoutId;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payWay")})
    private Integer payWay;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("date_add")})
    private Long dateAdd;
    private BigDecimal sum;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("sum_to_receive")})
    private BigDecimal sumToReceive;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("pay_status")})
    private Integer payStatus;

    public String getFormattedDateAdd() {
        return Util.formatTimestamp(dateAdd);
    }
}