package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class PaymentSystem {
    private int id;
    private String code;
    private String name;
    private String category;
    private List<String> availableCurrencies;
    private String iconUrl;
    private String fieldName;
    private String fieldType;
    private String fieldMask;
    private String fieldPlaceholder;
    private boolean active;
}