package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.Balance;
import dev.se1dhe.bot.model.DbUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    Optional<Balance> findByUser(DbUser user);
}