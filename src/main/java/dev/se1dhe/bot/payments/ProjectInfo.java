package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
public class ProjectInfo {
    @Getter
    private String status;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("comsn_from_payer")})
    private Integer comsnFromPayer;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("comsn_from_webmaster")})
    private Integer comsnFromWebmaster;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payWays")})
    private Map<String, Map<String, PayWayInfo>> payWays;

}