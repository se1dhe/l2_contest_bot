package dev.se1dhe.bot.payments;

import lombok.Data;

@Data
public class InitPayoutResponse {
    private String status;
    private PayoutResult result;
}