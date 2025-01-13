package dev.se1dhe.bot.service;

import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.GameUser;
import dev.se1dhe.bot.repository.GameUserRepository;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.util.BotUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

import static dev.se1dhe.bot.BotApplication.telegramBot;

@Service
public class NotificationService {

    private final GameUserRepository gameUserRepository;
    private final GameUserService gameUserService;

    @Autowired
    public NotificationService(GameUserRepository gameUserRepository, GameUserService gameUserService) {
        this.gameUserRepository = gameUserRepository;
        this.gameUserService = gameUserService;
    }

    @Transactional
    public void notifyActivatedUsers() {
        List<GameUser> usersToNotify = gameUserRepository.findByActiveTrueAndNotifiedFalse();

        for (GameUser user : usersToNotify) {
            DbUser dbUser = user.getDbUser();
            Long telegramId = dbUser.getId();
            String messageText = "Ваш персонаж " + user.getCharId() + " на сервере " + user.getServerName() + " успешно привязан!";



            try {
                BotUtil.sendHtmlMessageById(telegramBot, dbUser.getId().toString(), messageText, null);
                gameUserService.markAsNotified(dbUser.getId());
                
            } catch (TelegramApiException e) {
                System.err.println("Ошибка отправки уведомления пользователю " + telegramId + ": " + e.getMessage());
            }
        }
    }
    @Scheduled(fixedRate = 60000)
    public void notifyActivatedUsersScheduled() {
        notifyActivatedUsers();
    }
}