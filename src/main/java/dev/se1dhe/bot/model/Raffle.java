package dev.se1dhe.bot.model;


import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.bot.service.LocalizationService;
import lombok.*;
import utils.Util;

import jakarta.persistence.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
public class Raffle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    private String name;
    private String description;
    private int winnerCount;


    private String siteUrl;
    private String imgPath;
    private String channelForSub;
    private LocalDateTime startDate;
    private LocalDateTime raffleResultDate;

    @Enumerated(EnumType.STRING)
    private RaffleType type;

    private boolean participationBonus;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_raffle",
            joinColumns = {@JoinColumn(name = "raffle_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private List<DbUser> participant = new ArrayList<>();


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "raffle_prize",
            joinColumns = {@JoinColumn(name = "raffle_id")},
            inverseJoinColumns = {@JoinColumn(name = "prize_id")})
    private List<Prize> prizes = new ArrayList<>();
    public String start() throws IOException {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizes) {
            s.append(Util.getEmoji(prize.getPlace()))
                    .append(LocalizationService.getString("raffle.place"))
                    .append(" ")
                    .append(prize.getCount())
                    .append(" ")
                    .append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"), Config.BONUS_ITEM_COUNT, Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }

        // Исправление: добавлен недостающий аргумент для канала подписки
        return String.format(LocalizationService.getString("raffle.startMessage"),
                siteUrl, id, description, s.toString(), 0, winnerCount, siteUrl, siteUrl, channelForSub, Util.dateTimeParser(raffleResultDate));
    }





    public List<DbUser> chooseWinners() {
        List<DbUser> winners = new ArrayList<>();
        List<DbUser> participants = this.getParticipant();
        int numWinners = this.getWinnerCount();

        Random random = new Random();
        while (winners.size() < numWinners) {
            int randomIndex = random.nextInt(participants.size());
            DbUser winner = participants.get(randomIndex);
            if (!winners.contains(winner)) {
                winners.add(winner);
            }
        }
        return winners;
    }


    public String start(List<Prize> prizesByRaffle) {
        StringBuilder s = new StringBuilder();
        for (Prize prize : prizesByRaffle) {
            s.append(Util.getEmoji(prize.getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(prize.getCount()).append(" "). append(prize.getItemName());
        }
        if (Config.DAILY_PARTICIPANT_BONUS) {
            if (Config.ITEM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusItem.text"),Config.BONUS_ITEM_COUNT,Config.BONUS_ITEM_NAME, Config.PREMIUM_HOUR));
            }
            if (Config.PREMIUM_ENABLE) {
                s.append(String.format(LocalizationService.getString("bonusPremium.text"), Config.PREMIUM_HOUR));
            }
        }

        return String.format(LocalizationService.getString("delay.startMessage"), name, description, s, 0, winnerCount, siteUrl,siteUrl,channelForSub, Util.dateTimeParser(raffleResultDate));
    }
}
