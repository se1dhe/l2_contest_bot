package dev.se1dhe.bot.payments;

import lombok.Data;

@Data
public class RefundResponse {
    private String status;
    private RefundResult result;
}