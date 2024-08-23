
package dev.se1dhe.bot.service;


import dev.se1dhe.bot.model.RaffleBonus;
import dev.se1dhe.bot.repository.RaffleBonusRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RaffleBonusService {

    private final RaffleBonusRepository raffleBonusRepository;

    public RaffleBonusService(RaffleBonusRepository raffleBonusRepository) {
        this.raffleBonusRepository = raffleBonusRepository;
    }


    public void create(Long raffleId, Long dbUserId) {
        RaffleBonus raffleBonus = new RaffleBonus();
        raffleBonus.setRaffleId(raffleId);
        raffleBonus.setDbUserId(dbUserId);
        raffleBonusRepository.save(raffleBonus);
    }

    public RaffleBonus findRaffleBonusByRaffleIdAndDbUserId(Long raffleId, Long dbUserId) {
        return raffleBonusRepository.findRaffleBonusByRaffleIdAndDbUserId(raffleId, dbUserId);
    }

    public List<RaffleBonus> findAll() {
        return raffleBonusRepository.findAll();
    }
}
