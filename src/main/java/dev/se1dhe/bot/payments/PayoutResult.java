package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

@Data
public class PayoutResult {
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("payout_id")})
    private Long payoutId;
}