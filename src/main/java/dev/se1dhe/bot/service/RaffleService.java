package dev.se1dhe.bot.service;

import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.bot.repository.DbUserRepository;
import dev.se1dhe.bot.repository.RaffleRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RaffleService {
    
    private final RaffleRepository raffleRepository;
    private final DbUserRepository dbUserRepository;

    public RaffleService(RaffleRepository raffleRepository, DbUserRepository dbUserRepository) {
        this.raffleRepository = raffleRepository;
        this.dbUserRepository = dbUserRepository;
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


    @Transactional
    public void removeParticipant(Long raffleId, Long userId) {
        Raffle raffle = raffleRepository.findById(raffleId).orElse(null);
        if (raffle != null) {
            DbUser user = raffle.getParticipant().stream()
                    .filter(u -> u.getId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (user != null) {
                raffle.getParticipant().remove(user);
                user.getRaffles().remove(raffle);

                raffleRepository.save(raffle);  // Сохранение изменений в Raffle
                dbUserRepository.save(user);    // Сохранение изменений в DbUser
            }
        }
    }

    // Добавьте этот метод в класс RaffleService
    public Page<Raffle> findNonWinningRafflesByUser(DbUser dbUser, Pageable pageable) {
        return raffleRepository.findNonWinningRafflesByUser(dbUser, pageable);
    }

}