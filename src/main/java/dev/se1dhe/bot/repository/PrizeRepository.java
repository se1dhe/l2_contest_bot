package dev.se1dhe.bot.repository;


import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Long> {
    List<Prize> findAllByRaffles(Raffle raffle);
}