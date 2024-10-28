package dev.se1dhe.bot.service;


import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.Prize;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import dev.se1dhe.bot.model.enums.RaffleType;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.KeyboardBuilder;
import utils.XMLParser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static utils.Util.getEmoji;
import static utils.XMLParser.modifyRaffleType;
import static utils.XMLParser.parseDelayedRaffle;

@Service
@PropertySource("file:config/dailyRaffle.properties")
@Log4j2
public class UpdateService {


    private final WinnerService winnerService;
    private final RaffleService raffleService;
    private final PrizeService prizeService;


    public UpdateService(WinnerService winnerService, RaffleService raffleService, PrizeService prizeService) {
        this.winnerService = winnerService;
        this.raffleService = raffleService;
        this.prizeService = prizeService;
    }


    @Scheduled(cron = "${daily.raffleTime}")
    public void updateDaily() throws TelegramApiException {
        if (Config.DAILY_RAFFLE) {
            dailyRaffle(BotApplication.telegramBot);
        }
    }

    @Scheduled(initialDelay = 10000, fixedRate = 10000)
    public void updateDelayed() throws TelegramApiException, IOException {
        delayRafflePublished(BotApplication.telegramBot);
    }

    @Scheduled(initialDelay = 1000, fixedRate = 10000)
    public void autoDelete() throws TelegramApiException, IOException {
        autoDeleteRaffle(BotApplication.telegramBot);
    }

    @Scheduled(initialDelay = 12000, fixedRate = 15500)
    public void updateDailyEnd() throws TelegramApiException, IOException {
        dailyEnd(BotApplication.telegramBot);
    }
    @Scheduled(initialDelay = 11000, fixedRate = 15000)
    public void updateDelayEnd() throws TelegramApiException, IOException {
        delayEnd(BotApplication.telegramBot);
    }

    private void dailyEnd (AbstractTelegramBot bot) throws TelegramApiException {
        if (raffleService.getAllRafflesByType(RaffleType.DAILY) != null) {
            for (Raffle raffle : raffleService.getAllRafflesByType(RaffleType.DAILY)) {
                if (LocalDateTime.now().isAfter(raffle.getRaffleResultDate())) {
                    if (raffle.getParticipant().size() >= raffle.getWinnerCount()) {
                        winnerService.chooseWinners(raffle);
                        BotUtil.sendHtmlMessageById(bot, Config.CHANNEL_ID, end(raffle), null);
                        raffle.setType(RaffleType.ENDED);
                        raffleService.update(raffle);
                    }
                }

            }
        }


    }

    private void delayEnd (AbstractTelegramBot bot) throws TelegramApiException {
        if (raffleService.getAllRafflesByType(RaffleType.PUBLISHED) != null) {
            for (Raffle raffle : raffleService.getAllRafflesByType(RaffleType.PUBLISHED)) {
                if (LocalDateTime.now().isAfter(raffle.getRaffleResultDate())) {
                    if (raffle.getParticipant().size() >= raffle.getWinnerCount()) {
                        winnerService.chooseWinners(raffle);
                        BotUtil.sendHtmlMessageById(bot, Config.CHANNEL_ID, endDelay(raffle), null);
                        raffle.setType(RaffleType.ENDED);
                        raffleService.update(raffle);
                    }
                }
            }
        }
    }

    private void autoDeleteRaffle (AbstractTelegramBot bot)  {
        if (Config.RAFFLE_AUTO_DELETE) {
            if (raffleService.getAllRafflesByType(RaffleType.ENDED) != null) {
                for (Raffle raffle : raffleService.getAllRafflesByType(RaffleType.ENDED)) {
                    if (raffle.getRaffleResultDate().plusDays(Config.RAFFLE_AUTO_DELETE_DAYS).isBefore(LocalDateTime.now())) {
                        List<Winner> winnerList =  winnerService.findByRaffle(raffle);
                        for (Winner winner : winnerList) {
                            winnerService.delete(winner);
                        }
                        raffleService.deleteRaffleById(raffle.getId());
                        for (Winner winner : winnerService.findByRaffle(raffle)) {
                            winnerService.delete(winner);
                        }
                        log.info("Удален конкурс {}", raffle.getName());
                    }
                }
            }

        }
    }



    private void delayRafflePublished(AbstractTelegramBot bot) throws IOException, TelegramApiException {
        if(parseDelayedRaffle().getType().equals(RaffleType.DELAY)) {
            Raffle raffle = new Raffle();
            raffleService.update(raffle);
            raffle.setName(parseDelayedRaffle().getName());
            raffle.setDescription(parseDelayedRaffle().getDescription());
            raffle.setType(RaffleType.CREATED);
            raffle.setStartDate(parseDelayedRaffle().getStartDate());
            raffle.setRaffleResultDate(parseDelayedRaffle().getRaffleResultDate());
            raffle.setChannelForSub(parseDelayedRaffle().getChannelForSub());
            raffle.setWinnerCount(parseDelayedRaffle().getWinnerCount());
            raffle.setImgPath(parseDelayedRaffle().getImgPath());
            raffle.setSiteUrl(parseDelayedRaffle().getSiteUrl());
            List<Prize> prizeList = parseDelayedRaffle().getPrizes();
            raffle.setParticipationBonus(true);
            for (Prize prize : prizeList) {
                prizeService.save(prize);
                raffle.getPrizes().add(prize);
            }
            raffleService.update(raffle);
            modifyRaffleType("ENDED");
        }

        List<Raffle> delayRaffleList = raffleService.getAllRafflesByType(RaffleType.CREATED);
        if (!delayRaffleList.isEmpty()&&raffleService.getAllRafflesByType(RaffleType.CREATED)!=null) {
            for (Raffle delayRaffle : delayRaffleList) {
                if (LocalDateTime.now().isAfter(delayRaffle.getStartDate())) {
                    BotUtil.sendPhotoById(bot, Config.CHANNEL_ID, delayRaffle.start(prizeService.getPrizesByRaffle(delayRaffle)), new File(delayRaffle.getImgPath()), KeyboardBuilder.inline()
                            .button(LocalizationService.getString("raffle.participation"), "raffle_id:" + delayRaffle.getId()).build());
                    delayRaffle.setType(RaffleType.PUBLISHED);
                    raffleService.update(delayRaffle);
                }
            }
        }

    }


    public void dailyRaffle(AbstractTelegramBot bot) throws TelegramApiException {
        Raffle raffle = new Raffle();
        raffleService.update(raffle);
        raffle.setName(LocalizationService.getString("daily.raffleName") + " №" + raffle.getId());
        raffle.setDescription(LocalizationService.getString("daily.raffleDesc"));
        raffle.setType(RaffleType.DAILY);
        raffle.setRaffleResultDate(LocalDateTime.now().plusDays(1).minusSeconds(30));
        raffle.setWinnerCount(XMLParser.createPrize().size());
        raffle.setChannelForSub(Config.DAILY_CHANNEL_FOR_SUB);
        List<Prize> prizeList = XMLParser.createPrize();
        raffle.setParticipationBonus(true);
        for (Prize prize : prizeList) {
            prizeService.save(prize);
            raffle.getPrizes().add(prize);
        }
        raffle.setSiteUrl(LocalizationService.getString("daily.raffleSiteUrl"));
        raffleService.update(raffle);

        try {
            BotUtil.sendHtmlMessageById(bot, Config.CHANNEL_ID, raffle.start(), KeyboardBuilder.inline()
                    .button(LocalizationService.getString("raffle.participation"), "raffle_id:" + raffle.getId()).build());
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }

    }


    public String end(Raffle raffle)  {
        StringBuilder s = new StringBuilder();
        for (Winner winner : winnerService.findByRaffle(raffle)) {
            s.append(getEmoji(winner.getPrize().getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(winner.getPrize().getCount()).append(" ").append(winner.getPrize().getItemName()).append(" - ").append("<a href=\"tg://user?id=").append(winner.getParticipant().getId()).append("\">").append(winner.getParticipant().getUserName()).append("</a>");
        }
        return String.format(LocalizationService.getString("raffle.endMessage"), raffle.getSiteUrl(), raffle.getId(), s);
    }

    public String endDelay(Raffle raffle)  {
        StringBuilder s = new StringBuilder();
        for (Winner winner : winnerService.findByRaffle(raffle)) {
            s.append(getEmoji(winner.getPrize().getPlace())).append(LocalizationService.getString("raffle.place")).append(" ").append(winner.getPrize().getCount()).append(" ").append(winner.getPrize().getItemName()).append(" - ").append("<a href=\"tg://user?id=").append(winner.getParticipant().getId()).append("\">").append(winner.getParticipant().getUserName()).append("</a>");
        }
        return String.format(LocalizationService.getString("delay.endMessage"), raffle.getName(), s);
    }

    public String endDelay1(Raffle raffle) {
        StringBuilder s = new StringBuilder();
        List<Winner> winners = winnerService.findByRaffle(raffle);

        for (Winner winner : winners) {
            // Формируем строку с местом и ссылкой на пользователя
            s.append(winner.getPrize().getPlace())
                    .append(" место - ")
                    .append("<a href=\"tg://user?id=")
                    .append(winner.getParticipant().getId())
                    .append("\">")
                    .append(winner.getParticipant().getUserName())
                    .append("</a>\n"); // Добавляем перевод строки для разделения записей
        }

        // Возвращаем сформированное сообщение
        return "Победители:\n" + s.toString();
    }
}
