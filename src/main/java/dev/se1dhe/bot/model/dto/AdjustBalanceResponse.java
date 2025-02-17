package dev.se1dhe.bot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdjustBalanceResponse {
    private String status;
    private String message;
    //private BigDecimal balance;
}