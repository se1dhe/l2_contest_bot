package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.RaffleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaffleRepository extends JpaRepository<Raffle, Long> {

    List<Raffle> findAllRafflesByType(RaffleType raffleType);

    Raffle findByName(String raffleName);

}