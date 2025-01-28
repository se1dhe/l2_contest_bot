package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

@Data
public class RefundResult {
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("order_id")})
    private Long orderId;
}