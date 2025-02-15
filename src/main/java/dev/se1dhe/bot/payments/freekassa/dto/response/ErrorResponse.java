package dev.se1dhe.bot.payments.freekassa.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {
    private String type;
    private String message;
}