package dev.se1dhe.bot.service;


import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.bot.repository.PrizeRepository;
import dev.se1dhe.bot.repository.RaffleRepository;
import dev.se1dhe.bot.repository.WinnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class WinnerService {
    private final WinnerRepository winnerRepository;
    private final PrizeRepository prizeRepository;
    private final RaffleRepository raffleRepository;

    private final Random random;

    public WinnerService(WinnerRepository winnerRepository, PrizeRepository prizeRepository, RaffleRepository raffleRepository) {
        this.winnerRepository = winnerRepository;
        this.prizeRepository = prizeRepository;
        this.raffleRepository = raffleRepository;
        this.random = new Random();
    }


    public void chooseWinners(Raffle raffle) {
        List<DbUser> participants = raffle.getParticipant();
        int numWinners = raffle.getWinnerCount();

        List<DbUser> winners = new ArrayList<>();
        while (winners.size() < numWinners) {
            int randomIndex = random.nextInt(participants.size());
            DbUser winner = participants.get(randomIndex);
            if (!winners.contains(winner)) {
                winners.add(winner);
            }
        }

        List<Prize> prizes = prizeRepository.findAllByRaffles(raffle);

        for (int i = 0; i < prizes.size(); i++) {
            Prize prize = prizes.get(i);
            DbUser winner = winners.get(i);

            Winner winnerObject = new Winner();
            winnerObject.setRaffle(raffle);
            winnerObject.setParticipant(winner);
            winnerObject.setPrize(prize);
            winnerRepository.save(winnerObject);
        }

        raffle.setType(RaffleType.ENDED);
        raffleRepository.save(raffle);
    }


    public List<Winner> findByRaffle(Raffle raffle) {
        return winnerRepository.findByRaffle(raffle);
    }

    public List<Winner> findAllByRaffleIdAndParticipantId(Long raffleId, Long participantId ) {
        return winnerRepository.findAllByRaffleIdAndParticipantId(raffleId, participantId);
    }

    public Winner findByRaffleIdAndParticipantId(Long raffleId, Long participantId ) {
        return winnerRepository.findByRaffleIdAndParticipantId(raffleId, participantId);
    }

    public Page<Raffle> findUnclaimedPrizesByUser(DbUser user, Pageable pageable) {
        List<Winner> winners = winnerRepository.findByParticipant(user);
        List<Raffle> rafflesWithUnclaimedPrizes = winners.stream()
                .filter(winner -> !winner.isGetPrize())
                .map(Winner::getRaffle)
                .distinct()
                .collect(Collectors.toList());

        // Создаем Page из списка конкурсов
        int total = rafflesWithUnclaimedPrizes.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), total);
        List<Raffle> pageRaffles = rafflesWithUnclaimedPrizes.subList(start, end);

        return new PageImpl<>(pageRaffles, pageable, total);
    }


    public List<Winner> findByPrize(Prize prize) {
        return winnerRepository.findByPrize(prize);
    }

    public List<Winner> findAll() {
        return winnerRepository.findAll();
    }

    public Winner findById(Long winnerId) {
        return winnerRepository.findById(winnerId).orElse(null);
    }

    public void update(Winner winner) {
        winnerRepository.save(winner);
    }



    public Winner findByPrizeIdAndParticipantId(Long prizeId, Long userId) {
        return winnerRepository.findByPrizeIdAndParticipantId(prizeId, userId);
    }

    public void delete(Winner winner) {
        winnerRepository.delete(winner);
    }

    public Page<Raffle> findAllByParticipantId(Long participantId, Pageable pageable) {
        // Получаем страницу всех побед по ID участника
        Page<Winner> winnersPage = winnerRepository.findAllByParticipantId(participantId, pageable);

        // Извлекаем уникальные конкурсы из списка победителей
        List<Raffle> raffles = winnersPage.getContent().stream()
                .map(Winner::getRaffle)
                .distinct()
                .collect(Collectors.toList());

        // Создаем Page<Raffle> из списка уникальных конкурсов
        return new PageImpl<>(raffles, pageable, winnersPage.getTotalElements());
    }


}