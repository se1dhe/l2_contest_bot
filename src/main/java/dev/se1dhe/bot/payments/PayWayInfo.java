package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

@Data
public class PayWayInfo {
    private String comission;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("fix_sum")})
    private String fixSum;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("comission_RU")})
    private String comissionRU;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("fix_sum_RU")})
    private String fixSumRU;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("min_sum")})
    private String minSum;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("max_sum")})
    private String maxSum;
    private String comment;
}