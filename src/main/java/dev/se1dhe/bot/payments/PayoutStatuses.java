package dev.se1dhe.bot.payments;

import lombok.Getter;

public enum PayoutStatuses {
    PROCESSING(0),
    FAILED(-1),
    SUCCESS(1),
    PARTIALLY_PAID(2);

    @Getter
    private final int value;

    PayoutStatuses(int value) {
        this.value = value;
    }
}