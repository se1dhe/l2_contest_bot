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
                                                .text(LocalizationService.getString("contact.button", dbUser.getLang()))
                                                .callbackData("contact")
                                                .build()

                                ))

                        )
                )
                .build();
    }

    /*public static InlineKeyboardMarkup buildChannelListMenu(Page<Channel> channelsPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();

        channelsPage.getContent().forEach(channel -> {
            InlineKeyboardButton channelButton = InlineKeyboardButton.builder()
                    .text(channel.getName())
                    .callbackData("channel-address:" + channel.getAddress())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(channelButton);
            keyboardRows.add(row);
        });

        if (channelsPage.getTotalPages() > 1) {
            InlineKeyboardRow paginationRow = new InlineKeyboardRow();

            if (channelsPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang()))
                        .callbackData("channel-filter-page:" + (channelsPage.getNumber() - 1))
                        .build());
            }

            if (channelsPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang()))
                        .callbackData("channel-filter-page:" + (channelsPage.getNumber() + 1))
                        .build());
            }

            keyboardRows.add(paginationRow);
        }

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    public static InlineKeyboardMarkup buildContestListMenu(Page<Contest> contestsPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();

        contestsPage.getContent().forEach(contest -> {
            InlineKeyboardButton contestButton = InlineKeyboardButton.builder()
                    .text(Util.formatTime(contest.getEndDate()))
                    .callbackData("control-contest-id:" + contest.getId())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(contestButton);
            keyboardRows.add(row);
        });

        if (contestsPage.getTotalPages() > 1) {
            InlineKeyboardRow paginationRow = new InlineKeyboardRow();

            if (contestsPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang()))
                        .callbackData("contest-filter-page:" + (contestsPage.getNumber() - 1))
                        .build());
            }

            if (contestsPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang()))
                        .callbackData("contest-filter-page:" + (contestsPage.getNumber() + 1))
                        .build());
            }

            keyboardRows.add(paginationRow);
        }

        // Добавление кнопки "Назад"
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text("Назад")
                .callbackData("main-menu")
                .build());

        keyboardRows.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }*/

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

            // Кнопка "Назад", если есть предыдущая страница
            if (serversPage.hasPrevious()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.back", dbUser.getLang())) // Локализованный текст для кнопки назад
                        .callbackData("server-filter-page:" + (serversPage.getNumber() - 1)) // Callback для перехода на предыдущую страницу
                        .build());
            }

            // Кнопка "Вперед", если есть следующая страница
            if (serversPage.hasNext()) {
                paginationRow.add(InlineKeyboardButton.builder()
                        .text(LocalizationService.getString("registerMenu.forward", dbUser.getLang())) // Локализованный текст для кнопки вперед
                        .callbackData("server-filter-page:" + (serversPage.getNumber() + 1)) // Callback для перехода на следующую страницу
                        .build());
            }

            // Добавляем строку с кнопками пагинации в общий список
            keyboardRows.add(paginationRow);
        }

        // Создаем и возвращаем объект InlineKeyboardMarkup с заполненными строками
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    // Метод для создания меню с конкурсами, в которых пользователь победил
    public static InlineKeyboardMarkup buildUserWinningsMenu(Page<Raffle> rafflesPage, DbUser dbUser) {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();

        // Добавляем кнопки для каждого конкурса
        rafflesPage.getContent().forEach(raffle -> {
            InlineKeyboardButton raffleButton = InlineKeyboardButton.builder()
                    .text(raffle.getName())
                    .callbackData("raffle-details:" + raffle.getId())
                    .build();
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(raffleButton);
            keyboardRows.add(row);
        });

        // Добавляем кнопки для пагинации, если есть несколько страниц
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

        // Кнопка возврата в основное меню
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(InlineKeyboardButton.builder()
                .text(LocalizationService.getString("mainMenu.button", dbUser.getLang()))
                .callbackData("start-menu")
                .build());

        keyboardRows.add(backRow);

        // Создаем и возвращаем клавиатуру
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

}
