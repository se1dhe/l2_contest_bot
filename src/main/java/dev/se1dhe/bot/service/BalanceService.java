package dev.se1dhe.bot.service;

import dev.se1dhe.bot.exception.InsufficientFundsException;
import dev.se1dhe.bot.exception.UserNotFoundException;
import dev.se1dhe.bot.exception.WithdrawException; // Импортируем WithdrawException
import dev.se1dhe.bot.model.Balance;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Transaction;
import dev.se1dhe.bot.repository.BalanceRepository;
import dev.se1dhe.bot.repository.TransactionRepository;
import dev.se1dhe.bot.repository.DbUserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.se1dhe.bot.model.enums.TransactionType.DEPOSIT;
import static dev.se1dhe.bot.model.enums.TransactionType.WITHDRAWAL;

@Service
@Log4j2
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final DBUserService dbUserService;

    @Autowired
    public BalanceService(BalanceRepository balanceRepository, TransactionRepository transactionRepository, DBUserService dbUserService) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.dbUserService = dbUserService;
    }

    public Balance getBalanceById(Long balanceId) {
        Optional<Balance> balanceOptional = balanceRepository.findById(balanceId);
        return balanceOptional.orElse(null);  //  return balanceOptional.orElseThrow(() -> new BalanceNotFoundException("Balance with id " + balanceId + " not found"));  --  более правильный вариант в production
    }
    @Transactional
    public void deposit(Long userId, BigDecimal amount) {
        DbUser user = dbUserService.findUserById(userId);
        Balance balance = getBalanceByUser(user);
        if(balance != null) {
            balance.addFunds(amount);
            balanceRepository.save(balance);  // Сохраняем изменения баланса
            transactionRepository.save(new Transaction(balance, amount, DEPOSIT));
        } else {
            log.error("Не удалось найти баланс пользователя с ID: {}", userId);

        }
    }

    @Transactional
    public void withdraw(Long userId, BigDecimal amount) throws InsufficientFundsException, UserNotFoundException {
        DbUser user = dbUserService.findUserById(userId);
        Balance balance = getBalanceByUser(user);
        if (balance == null) {
            log.error("Balance for user with ID {} not found.", userId);
            throw new IllegalStateException("Balance for user with ID " + userId + " not found.");
        }

        if (balance.getAmount().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на балансе пользователя " + userId);
        }

        balance.withdrawFunds(amount);
        balanceRepository.save(balance);
        transactionRepository.save(new Transaction(balance, amount, WITHDRAWAL));
    }


    public BigDecimal getBalance(Long userId) {
        DbUser user = dbUserService.findUserById(userId); // Может выбросить UserNotFoundException
        Balance balance = getBalanceByUser(user); // Может вернуть null, нужно обработать
        if (balance != null) {
            return balance.getAmount();
        } else {
            log.error("Balance not found for user ID: {}", userId);
            return BigDecimal.ZERO; //  0, или выбросить исключение
        }
    }
    private Balance getBalanceByUser(DbUser user) {
        Optional<Balance> balanceOptional = balanceRepository.findByUser(user);
        return balanceOptional.orElseGet(() -> {
            Balance newBalance = new Balance(user); //Создаем баланс если не был создан
            return balanceRepository.save(newBalance);
        });
    }
}