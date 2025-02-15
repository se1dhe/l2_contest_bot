package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class CurrenciesResponse {
    private String status;
    private List<Currency> data;
}