package dev.se1dhe.bot.payments.fkwallet.dto.request;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Builder
@Getter
@Setter
public class WithdrawFkWalletRequest {
    private BigDecimal amount;
    private int currency;  // Изменил тип на int, так как это ID
    private int paymentSystemId; // Изменил название на paymentSystemId
    private int feeFromBalance;  //Изменил тип
    private String account;
    private String description;
    private Integer orderId; // Добавлено поле для order_id, может быть null
    private String idempotenceKey; // Добавлено поле для idempotence_key

}