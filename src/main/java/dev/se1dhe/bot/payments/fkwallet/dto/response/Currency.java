package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class Currency {
    private int id;
    private String code;
    private BigDecimal course;
}