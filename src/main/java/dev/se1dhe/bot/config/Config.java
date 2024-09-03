package dev.se1dhe.bot.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import utils.PropertiesParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class Config {

    // Конфигурационные файлы
    public static final String CONFIGURATION_BOT_FILE = "config/bot.properties";
    public static final String CONFIGURATION_DB_FILE = "config/database.properties";
    public static final String CONFIGURATION_DAILY_RAFFLE_FILE = "config/dailyRaffle.properties";

    // Параметры бота
    public static String BOT_NAME;
    public static String BOT_TOKEN;
    public static String CHANNEL_ID;
    public static String LANGUAGE;
    public static String SERVER_COMMAND_NAME;
    public static boolean RAFFLE_AUTO_DELETE;
    public static int RAFFLE_AUTO_DELETE_DAYS;
    public static int ITEM_ON_PAGE;

    // Параметры ежедневного розыгрыша
    public static String DAILY_RAFFLE_TIME;
    public static String DAILY_CHANNEL_FOR_SUB;
    public static boolean DAILY_RAFFLE;
    public static boolean DAILY_PARTICIPANT_BONUS;
    public static int BONUS_ITEM_ID;
    public static String BONUS_ITEM_NAME;
    public static int BONUS_ITEM_COUNT;
    public static boolean PREMIUM_ENABLE;
    public static boolean ITEM_ENABLE;
    public static int PREMIUM_HOUR;

    // Параметры базы данных
    public static String DB_URL;
    public static String DB_USER;
    public static String DB_PWD;

    // Параметры конфигурации для игровых рейтов
    public static double RATE_XP;
    public static double RATE_SP;
    public static double RATE_RAID_XP;
    public static double RATE_RAID_SP;
    public static double QUEST_REWARD_RATE;
    public static double QUEST_REWARD_ADENA_RATE;
    public static double QUEST_DROP_RATE;
    public static double DROP_ADENA;
    public static double DROP_ITEMS;
    public static double DROP_SEAL_STONES;
    public static double DROP_RAID_ITEMS;
    public static double DROP_SPOIL;

    // Параметры серверов
    private static Map<String, ServerConfig> serverConfigs = new HashMap<>();
    @Getter
    private static ServerConfig currentServerConfig;

    // Класс для хранения конфигурации сервера
    public static class ServerConfig {
        public String name; // Имя сервера
        public String url;
        public String username;
        public String password;
        public String serverType; // Тип сборки

        public ServerConfig(String name, String url, String username, String password, String serverType) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
            this.serverType = serverType;
        }
    }

    // Метод для загрузки конфигурации
    public static void load() {
        final PropertiesParser botConfig = new PropertiesParser(CONFIGURATION_BOT_FILE);
        final PropertiesParser botDBConfig = new PropertiesParser(CONFIGURATION_DB_FILE);
        final PropertiesParser dailyRaffleConfig = new PropertiesParser(CONFIGURATION_DAILY_RAFFLE_FILE);

        // Загрузка параметров бота
        BOT_NAME = botConfig.getString("bot.name", "contest_bot");
        BOT_TOKEN = botConfig.getString("bot.token", "1590228823:AAE5CS0GZXyOEFj_wreUV48vGclDmSIcdjA");
        CHANNEL_ID = botConfig.getString("bot.channel_id", "-1001600979960");
        LANGUAGE = botConfig.getString("bot.language", "ru");
        SERVER_COMMAND_NAME = botConfig.getString("bot.serverName", "lucera2");
        RAFFLE_AUTO_DELETE = botConfig.getBoolean("bot.raffleAutoDelete", false);
        RAFFLE_AUTO_DELETE_DAYS = botConfig.getInt("bot.autoDeleteDays", 3);
        ITEM_ON_PAGE = botConfig.getInt("bot.itemOnPage", 3);

        // Загрузка параметров ежедневного розыгрыша
        DAILY_RAFFLE = dailyRaffleConfig.getBoolean("daily.raffleEnable", true);
        DAILY_RAFFLE_TIME = dailyRaffleConfig.getString("daily.raffleTime", "0 55 02 * * ?");
        DAILY_CHANNEL_FOR_SUB = dailyRaffleConfig.getString("daily.channelForSub", "@se1dhe_dev");
        DAILY_PARTICIPANT_BONUS = dailyRaffleConfig.getBoolean("daily.participationBonus", true);
        BONUS_ITEM_ID = dailyRaffleConfig.getInt("daily.bonusItemId", 57);
        BONUS_ITEM_NAME = dailyRaffleConfig.getString("daily.bonusItemName", "Adena");
        BONUS_ITEM_COUNT = dailyRaffleConfig.getInt("daily.bonusItemCount", 500000);
        PREMIUM_ENABLE = dailyRaffleConfig.getBoolean("daily.bonusPremium", true);
        ITEM_ENABLE = dailyRaffleConfig.getBoolean("daily.bonusItem", true);
        PREMIUM_HOUR = dailyRaffleConfig.getInt("daily.bonusPremiumHour", 24);

        // Загрузка игровых рейтов из конфигурации
        RATE_XP = botConfig.getDouble("rateXp", 1.5);
        RATE_SP = botConfig.getDouble("rateSp", 1.5);
        RATE_RAID_XP = botConfig.getDouble("rateRaidXp", 1.5);
        RATE_RAID_SP = botConfig.getDouble("rateRaidSp", 1.5);
        QUEST_REWARD_RATE = botConfig.getDouble("questRewardRate", 1.5);
        QUEST_REWARD_ADENA_RATE = botConfig.getDouble("questRewardAdenaRate", 1.5);
        QUEST_DROP_RATE = botConfig.getDouble("questDropRate", 1.5);
        DROP_ADENA = botConfig.getDouble("dropAdena", 1.5);
        DROP_ITEMS = botConfig.getDouble("dropItems", 1.5);
        DROP_SEAL_STONES = botConfig.getDouble("dropSealStones", 1.5);
        DROP_RAID_ITEMS = botConfig.getDouble("dropRaidItems", 1.5);
        DROP_SPOIL = botConfig.getDouble("dropSpoil", 1.5);

        // Загрузка параметров базы данных
        DB_URL = botDBConfig.getString("bot.url", "jdbc:mysql://127.0.0.1:3306/raffle?useUnicode=true&character_set_server=utf8mb4&autoReconnect=true&interactiveClient=true&serverTimezone=Europe/Kiev&useSSL=false");
        DB_USER = botDBConfig.getString("bot.username", "root");
        DB_PWD = botDBConfig.getString("bot.password", "1234");

        // Загрузка конфигураций серверов
        loadServerConfigs(botDBConfig);

        // Устанавливаем первый сервер как текущий (по умолчанию)
        if (!serverConfigs.isEmpty()) {
            currentServerConfig = serverConfigs.values().iterator().next();
        }
    }

    // Метод для загрузки конфигураций серверов
    private static void loadServerConfigs(PropertiesParser botDBConfig) {
        // Получаем все ключи из конфигурационного файла
        Set<String> keys = botDBConfig.getKeys();

        // Фильтруем ключи, чтобы найти все серверы по ключу 'serverX.name'
        List<String> serverNames = keys.stream()
                .filter(key -> key.matches("server\\d+\\.name"))
                .toList();

        // Загружаем все конфигурации серверов, которые найдены
        for (String serverNameKey : serverNames) {
            // Извлекаем номер сервера из ключа (например, из 'server1.name' получаем '1')
            String serverNumber = serverNameKey.replaceAll("\\D+", ""); // оставляем только цифры
            String prefix = "server" + serverNumber;

            // Читаем параметры сервера по найденному номеру
            String name = botDBConfig.getString(prefix + ".name", "Server " + serverNumber);
            String url = botDBConfig.getString(prefix + ".url", "");
            String username = botDBConfig.getString(prefix + ".username", "root");
            String password = botDBConfig.getString(prefix + ".password", "1234");
            String type = botDBConfig.getString(prefix + ".serverType", "l2server");

            // Проверка, что обязательные параметры не пустые
            if (!url.isEmpty() && !name.isEmpty()) {
                serverConfigs.put(name, new ServerConfig(name, url, username, password, type));
            }
        }
    }

    // Метод для получения списка доступных серверов
    public static List<ServerConfig> getAvailableServers() {
        // Возвращаем список объектов ServerConfig из serverConfigs
        return new ArrayList<>(serverConfigs.values());
    }

    // Пример использования метода для вывода списка серверов
    public static void printAvailableServers() {
        List<ServerConfig> servers = getAvailableServers();
        if (servers.isEmpty()) {
            log.info("Доступных серверов нет.");
        } else {
            servers.forEach(server -> log.info("Доступный сервер: " + server.name));
        }
    }

    // Метод для переключения текущего сервера по имени
    public static void switchServer(String serverName) {
        ServerConfig config = serverConfigs.get(serverName);
        if (config != null) {
            currentServerConfig = config;
            log.info("Переключились на сервер: " + config.name);
        } else {
            log.warn("Сервер с именем \"" + serverName + "\" не найден.");
        }
    }
}
