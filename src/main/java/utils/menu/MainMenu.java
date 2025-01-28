package utils.menu;

import dev.se1dhe.bot.config.Config;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.model.Raffle;
import dev.se1dhe.bot.model.Winner;
import dev.se1dhe.bot.service.LocalizationService;
import org.springframework.data.domain.Page;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import utils.KeyboardBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainMenu {


    public static InlineKeyboardMarkup mainMenu(DbUser dbUser) {
        return InlineKeyboardMarkup.builder()
                .keyboard(
                        Arrays.asList(
                                new InlineKeyboardRow(Arrays.asList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("start.getPrize", dbUser.getLang()))
                                                .callbackData("reward")
                                                .build(),
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("start.getBonus", dbUser.getLang()))
                                                .callbackData("bonus")
                                                .build()
                                )),
                                new InlineKeyboardRow(Collections.singletonList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("balance.menu.button", dbUser.getLang()))
                                                .callbackData("balance")
                                                .build()
                                )),
                                new InlineKeyboardRow(Collections.singletonList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("addCharacter.button", dbUser.getLang()))
                                                .callbackData("add-character")
                                                .build()

                                )),
                                new InlineKeyboardRow(Collections.singletonList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("addChar.deleteButton", dbUser.getLang()))
                                                .callbackData("del-all-character")
                                                .build()

                                )), new InlineKeyboardRow(Collections.singletonList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("vote.button", dbUser.getLang()))
                                                .url("https://t.me/L2Vote_bot?start=89")
                                                .build()

                                )),
                                new InlineKeyboardRow(Collections.singletonList(
                                        InlineKeyboardButton.builder()
                                                .text(LocalizationService.getString("contact.button", dbUser.getLang()))
                                                .callbackData("contact")
                                                .build()

                                ))

                        )
                )
                .build();
    }



    public static InlineKeyboardMarkup buildServerListMenu(Page<Config.ServerConfig> serversPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();

        serversPage.getContent().forEach(server -> {
            InlineKeyboardButton serverButton = InlineKeyboardButton.builder()
                    .text(server.name)
                    .callbackData("server-name:" + server.name)
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(serverButton);
            keyboardRows.add(row);
        });

        if (serversPage.getTotalPages() > 1) {
            InlineKeyboardRow paginationRow = new InlineKeyboardRow();

            if (serversPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang())) // Локализованный текст для кнопки назад
                        .callbackData("server-filter-page:" + (serversPage.getNumber() - 1)) // Callback для перехода на предыдущую страницу
                        .build());
            }

            if (serversPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang())) // Локализованный текст для кнопки вперед
                        .callbackData("server-filter-page:" + (serversPage.getNumber() + 1)) // Callback для перехода на следующую страницу
                        .build());
            }

            keyboardRows.add(paginationRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public static InlineKeyboardMarkup buildUserWinningsMenu(Page<Raffle> rafflesPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();

        rafflesPage.getContent().forEach(raffle -> {
            InlineKeyboardButton raffleButton = InlineKeyboardButton.builder()
                    .text(raffle.getName())
                    .callbackData("raffle-details:" + raffle.getId())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(raffleButton);
            keyboardRows.add(row);
        });

        if (rafflesPage.getTotalPages() > 1) {
            InlineKeyboardRow paginationRow = new InlineKeyboardRow();

            if (rafflesPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang()))
                        .callbackData("raffle-page:" + (rafflesPage.getNumber() - 1))
                        .build());
            }

            if (rafflesPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang()))
                        .callbackData("raffle-page:" + (rafflesPage.getNumber() + 1))
                        .build());
            }

            keyboardRows.add(paginationRow);
        }

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text(LocalizationService.getString("mainMenu.button", dbUser.getLang()))
                .callbackData("start-menu")
                .build());

        keyboardRows.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public static InlineKeyboardMarkup buildNonWinningRafflesMenu(Page<Raffle> rafflesPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();
        rafflesPage.getContent().forEach(raffle -> {
            InlineKeyboardButton raffleButton = InlineKeyboardButton.builder()
                    .text(raffle.getName())
                    .callbackData("bonus-raffle-details:" + raffle.getId())  // Исправлено
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(raffleButton);
            keyboardRows.add(row);
        });
        if (rafflesPage.getTotalPages() > 1) {
            InlineKeyboardRow paginationRow = new InlineKeyboardRow();

            if (rafflesPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang()))
                        .callbackData("bonus-raffle-page:" + (rafflesPage.getNumber() - 1))
                        .build());
            }

            if (rafflesPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang()))
                        .callbackData("bonus-raffle-page:" + (rafflesPage.getNumber() + 1))
                        .build());
            }

            keyboardRows.add(paginationRow);
        }
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text(LocalizationService.getString("mainMenu.button", dbUser.getLang()))
                .callbackData("start-menu")
                .build());

        keyboardRows.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

}


