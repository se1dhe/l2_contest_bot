package dev.se1dhe.bot.payments;

import lombok.Data;

@Data
public class PayoutInfoResponse {
    private String status;
    private PayoutInfo result;
}