package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
@Getter
@Setter
@Data
public class ErrorResponseFK {
    private String message;
    private Map<String, String[]> errors; // Изменил тип для errors
}