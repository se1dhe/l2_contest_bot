package dev.se1dhe.bot.handler;


import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.service.LocalizationService;
import dev.se1dhe.core.bots.AbstractTelegramBot;
import dev.se1dhe.core.handlers.ICallbackQueryHandler;
import dev.se1dhe.core.handlers.IMessageHandler;
import dev.se1dhe.core.util.BotUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.KeyboardBuilder;
import utils.menu.MainMenu;

@Service
@Log4j2
public class ContactHandler implements ICallbackQueryHandler{



    @Override
    public boolean onCallbackQuery(AbstractTelegramBot bot, Update update, CallbackQuery query) throws TelegramApiException {
        if (query.getData().equals("contact")) {
            BotUtil.editMessage(bot,query, LocalizationService.getString("contact.message"), false,
                    KeyboardBuilder.inline()
                            .button(LocalizationService.getString("mainMenu.button"), "start-menu")
                            .build()
                    );
        }
        return false;
    }

}
