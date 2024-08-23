

package dev.se1dhe.bot.repository;

import dev.se1dhe.bot.model.RaffleBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RaffleBonusRepository extends JpaRepository<RaffleBonus,Long> {
    RaffleBonus findRaffleBonusByRaffleIdAndDbUserId(Long raffleId, Long dbUserId);
}
