package dev.se1dhe.bot.model.dto;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

@Data
public class AdjustBalanceRequest {
    @NotNull
    private Long telegramId;

    @NotNull
    private BigDecimal amount;

    private String reason; // Optional
}