package dev.se1dhe.bot.model;

import dev.se1dhe.bot.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "balance_id", referencedColumnName = "id")
    private Balance balance;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    public Transaction(Balance balance, BigDecimal amount, TransactionType type) {
        this.balance = balance;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

}