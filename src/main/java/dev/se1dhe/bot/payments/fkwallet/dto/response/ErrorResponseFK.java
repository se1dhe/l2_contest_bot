package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class ErrorResponseFK {
    private String message;
    private Map<String, String[]> errors;
}