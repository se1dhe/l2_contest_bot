package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.RaffleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaffleRepository extends JpaRepository<Raffle, Long> {

    List<Raffle> findAllRafflesByType(RaffleType raffleType);

    Raffle findByName(String raffleName);

    @Query("SELECT r FROM Raffle r JOIN r.participant p WHERE p = :user AND NOT EXISTS (SELECT w FROM Winner w WHERE w.raffle = r AND w.participant = p)")
    Page<Raffle> findNonWinningRafflesByUser(DbUser user, Pageable pageable);

}