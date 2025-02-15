package dev.se1dhe.bot.payments.fkwallet.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Data
@Getter
@Setter
public class WithdrawFkWalletResponse {
    private String status;
    private String message;
    private Map<String, Object> data;
    private String type;
}