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
import java.util.Set;

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

                raffleRepository.save(raffle);
                dbUserRepository.save(user);
            }
        }
    }

    public Page<Raffle> findNonWinningRafflesByUser(DbUser dbUser, RaffleType type, Pageable pageable) {
        return raffleRepository.findNonWinningRafflesByUserAndType(dbUser, type, pageable);
    }

    @Transactional
    public void removeParticipantFromAllRaffles(Long userId) {
        DbUser user = dbUserRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        List<Raffle> raffles = user.getRaffles();
        for (Raffle raffle : new ArrayList<>(raffles)) {
            raffle.getParticipant().remove(user);
            raffleRepository.save(raffle);
        }
        user.getRaffles().clear();
        dbUserRepository.save(user);
    }
}
