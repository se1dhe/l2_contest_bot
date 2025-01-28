package dev.se1dhe.bot.model;

import dev.se1dhe.bot.exception.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "balance")
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private DbUser user;

    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;

    public Balance(DbUser user) {
        this.user = user;
    }

    public void addFunds(BigDecimal amount) {
        this.amount = this.amount.add(amount);
    }

    public void withdrawFunds(BigDecimal amount) {
        if (this.amount.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на балансе");
        }
        this.amount = this.amount.subtract(amount);
    }
}