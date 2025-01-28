package dev.se1dhe.bot.service;

import dev.se1dhe.bot.exception.InsufficientFundsException;
import dev.se1dhe.bot.exception.UserNotFoundException;
import dev.se1dhe.bot.model.Balance;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Transaction;
import dev.se1dhe.bot.repository.BalanceRepository;
import dev.se1dhe.bot.repository.TransactionRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        return balanceOptional.orElse(null);
    }

    @Transactional
    public void deposit(Long userId, BigDecimal amount) {
        DbUser user = dbUserService.findUserById(userId);
        if (user == null) {
            log.error("Пользователь с ID {} не найден.", userId);
            throw new UserNotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        Balance balance = getBalanceByUser(user);
        balance.addFunds(amount);
        balanceRepository.save(balance);
        transactionRepository.save(new Transaction(balance, amount, DEPOSIT));
    }

    @Transactional
    public void withdraw(Long userId, BigDecimal amount) {
        DbUser user = dbUserService.findUserById(userId);
        Balance balance = getBalanceByUser(user);
        balance.withdrawFunds(amount);
        balanceRepository.save(balance);
        transactionRepository.save(new Transaction(balance, amount, WITHDRAWAL));
    }

    public BigDecimal getBalance(Long userId) {
        DbUser user = dbUserService.findUserById(userId);
        Balance balance = getBalanceByUser(user);
        return balance.getAmount();
    }

    private Balance getBalanceByUser(DbUser user) {
        Optional<Balance> balanceOptional = balanceRepository.findByUser(user);
        return balanceOptional.orElseGet(() -> {
            Balance newBalance = new Balance(user);
            return balanceRepository.save(newBalance);
        });
    }
}