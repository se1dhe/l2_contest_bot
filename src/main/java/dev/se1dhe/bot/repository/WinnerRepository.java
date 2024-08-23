package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WinnerRepository extends JpaRepository<Winner, Long> {


        Winner save(Winner winner);
        List<Winner> findByRaffle(Raffle raffle);
        List<Winner> findByPrize(Prize prize);


    List<Winner> findAllByRaffleIdAndParticipantId(Long raffleId, Long participantId);
    Winner findByRaffleIdAndParticipantId(Long raffleId, Long participantId);



    Winner findByPrizeIdAndParticipantId(Long prizeId, Long userId);
}