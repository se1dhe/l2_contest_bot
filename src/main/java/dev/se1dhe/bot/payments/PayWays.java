package dev.se1dhe.bot.payments;

import lombok.Getter;

public enum PayWays {
    CARD(1),
    YANDEX_MONEY(2),
    WEBMONEY(3),
    USDT_TRC20(4),
    QIWI(5),
    MOBILE_PAYMENT(6),
    SBP(8);

    @Getter
    private final int value;

    PayWays(int value) {
        this.value = value;
    }
}