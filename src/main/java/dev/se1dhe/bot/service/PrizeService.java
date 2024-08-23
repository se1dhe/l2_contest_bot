package dev.se1dhe.bot.service;

import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.repository.PrizeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PrizeService {
    private final PrizeRepository prizeRepository;

    public PrizeService(PrizeRepository prizeRepository) {
        this.prizeRepository = prizeRepository;
    }

    public Prize save(Prize prize) {
        return prizeRepository.save(prize);
    }

    public List<Prize> getAllPrizes() {
        return prizeRepository.findAll();
    }

    public Optional<Prize> getPrizeById(Long id) {
        return prizeRepository.findById(id);
    }

    public void deletePrizeById(Long id) {
        prizeRepository.deleteById(id);
    }

    public List<Prize> getPrizesByRaffle(Raffle raffle) {
        return prizeRepository.findAllByRaffles(raffle);
    }
}