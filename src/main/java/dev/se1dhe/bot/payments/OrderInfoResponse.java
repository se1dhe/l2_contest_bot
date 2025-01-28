package dev.se1dhe.bot.payments;

import lombok.Data;

@Data
public class OrderInfoResponse {
    private String status;
    private OrderInfo result;
}