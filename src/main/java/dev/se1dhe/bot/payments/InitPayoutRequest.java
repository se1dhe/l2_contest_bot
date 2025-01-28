package dev.se1dhe.bot.payments;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class InitPayoutRequest {
    private long project;
    private BigDecimal sum;
    private String currency;
    private int payWay;
    private String email;
    private String purse;
    private String docNumber; // Дополнительно для карт
    private String cardholderFirstName; // Дополнительно для карт
    private String cardholderLastName; // Дополнительно для карт
    private String comment;
    private String needUnique;
    private String cardholder;
    private String sbpId;
}