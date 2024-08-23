package dev.se1dhe.bot.service;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.bot.repository.DbUserRepository;
import dev.se1dhe.bot.repository.RaffleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RaffleService {
    
    private final RaffleRepository raffleRepository;

    public RaffleService(RaffleRepository raffleRepository) {
        this.raffleRepository = raffleRepository;
    }
    
    public Raffle update(Raffle raffle) {
        return raffleRepository.save(raffle);
    }
    
    public List<Raffle> getAllRaffles() {
        return raffleRepository.findAll();
    }
    
    public Raffle getRaffleById(Long id) {
        return raffleRepository.findById(id).orElse(null);
    }
    
    public void deleteRaffleById(Long id) {
        raffleRepository.deleteById(id);
    }

    public List<Raffle> getAllRafflesByType(RaffleType raffleType) {
        return raffleRepository.findAllRafflesByType(raffleType);
    }

    public Raffle getRaffleByName(String raffleName) {
        return raffleRepository.findByName(raffleName);
    }


    public void create(Raffle raffle) {
        raffleRepository.save(raffle);
    }
}