package dev.se1dhe.bot.payments;

import lombok.Getter;

public enum OrderStatuses {
    PROCESSING(0),
    EXPIRED(-2),
    FAILED(-1),
    SUCCESS(1),
    REFUNDING(2),
    REFUNDED(3);

    @Getter
    private final int value;

    OrderStatuses(int value) {
        this.value = value;
    }
}