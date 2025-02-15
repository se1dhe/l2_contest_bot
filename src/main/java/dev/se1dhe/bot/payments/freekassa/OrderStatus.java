package dev.se1dhe.bot.payments.freekassa;

import lombok.Getter;

public enum OrderStatus {
    NEW(0),
    PAID(1),
    REFUND(6),
    ERROR(8),
    CANCELED(9);

    @Getter
    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }
}