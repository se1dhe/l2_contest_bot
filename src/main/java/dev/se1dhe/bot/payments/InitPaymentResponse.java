package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.Getter;

@Data
public class InitPaymentResponse {
    private String status;
    private String result;
    @Getter(onMethod_ = {@com.fasterxml.jackson.annotation.JsonProperty("order_id")})
    private Long orderId;
}