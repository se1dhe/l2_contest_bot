package dev.se1dhe.bot.service.dbManager;

import dev.se1dhe.bot.config.Config;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Lucera2DbManager extends Manager {
    public Lucera2DbManager(String url, String username, String password) throws SQLException {
        super(url, username, password);
    }

    @Override
    protected Connection getConnection(String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void addItem(int ownerId, int itemId, int count) throws SQLException {
        String query = "INSERT INTO `items_delayed` (`owner_id`, `item_id`, `count`, `enchant_level`, `variationId1`, `variationId2`, `attribute`, `attribute_level`, `flags`, `payment_status`, `description`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, ownerId);
            statement.setInt(2, itemId);
            statement.setInt(3, count);
            statement.setInt(4, 0); // enchant_level
            statement.setInt(5, 0); // variationId1
            statement.setInt(6, 0); // variationId2
            statement.setInt(7, -1); // attribute
            statement.setInt(8, -1); // attribute_level
            statement.setInt(9, 0); // flags
            statement.setString(10, "");

            statement.executeUpdate();
        }
    }

    @Override
    public void addPremiumData(int charId, int expireTimeHours) throws SQLException {
        // SQL запросы
        String getAccountQuery = "SELECT `account_name` FROM `characters` WHERE `obj_Id` = ?";
        String getPremiumDataQuery = "SELECT `expireTime` FROM `accounts_bonuses` WHERE `account` = ?";
        String insertBonusQuery = "INSERT INTO `accounts_bonuses` (`account`, `expireTime`, `rateXp`, `rateSp`, `rateRaidXp`, `rateRaidSp`, " +
                "`questRewardRate`, `questRewardAdenaRate`, `questDropRate`, `dropAdena`, `dropItems`, `dropSealStones`, `dropRaidItems`, " +
                "`dropSpoil`, `enchantItemBonus`, `enchantSkillBonus`, `hwidsLimit`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1.01, 1.05, 1)";
        String updateBonusQuery = "UPDATE `accounts_bonuses` SET `expireTime` = ? WHERE `account` = ?";

        // Преобразование часов в секунды
        int expireTimeSeconds = expireTimeHours * 3600;

        // Получаем текущее время в секундах с начала эпохи Unix
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        int expireTimeTimestamp = (int) (currentTimeSeconds + expireTimeSeconds);

        try (PreparedStatement getAccountStmt = connection.prepareStatement(getAccountQuery)) {
            getAccountStmt.setInt(1, charId);

            try (ResultSet resultSet = getAccountStmt.executeQuery()) {
                if (resultSet.next()) {
                    String accountName = resultSet.getString("account_name");

                    int currentExpireTimeSeconds = 0;

                    try (PreparedStatement getPremiumDataStmt = connection.prepareStatement(getPremiumDataQuery)) {
                        getPremiumDataStmt.setString(1, accountName);

                        try (ResultSet premiumResultSet = getPremiumDataStmt.executeQuery()) {
                            if (premiumResultSet.next()) {
                                currentExpireTimeSeconds = premiumResultSet.getInt("expireTime");
                            }
                        }
                    }

                    // Если запись уже существует, добавляем новое время к существующему
                    int newExpireTimeSeconds = currentExpireTimeSeconds > 0
                            ? currentExpireTimeSeconds + expireTimeSeconds
                            : expireTimeTimestamp;

                    try (PreparedStatement updateStatement = connection.prepareStatement(currentExpireTimeSeconds > 0 ? updateBonusQuery : insertBonusQuery)) {
                        if (currentExpireTimeSeconds > 0) {
                            // Если запись существует, обновляем её
                            updateStatement.setInt(1, newExpireTimeSeconds);
                            updateStatement.setString(2, accountName);
                        } else {
                            // Если запись не существует, вставляем новую
                            updateStatement.setString(1, accountName);
                            updateStatement.setInt(2, newExpireTimeSeconds);
                            // Передача игровых рейтов в SQL-запрос
                            updateStatement.setDouble(3, Config.RATE_XP);
                            updateStatement.setDouble(4, Config.RATE_SP);
                            updateStatement.setDouble(5, Config.RATE_RAID_XP);
                            updateStatement.setDouble(6, Config.RATE_RAID_SP);
                            updateStatement.setDouble(7, Config.QUEST_REWARD_RATE);
                            updateStatement.setDouble(8, Config.QUEST_REWARD_ADENA_RATE);
                            updateStatement.setDouble(9, Config.QUEST_DROP_RATE);
                            updateStatement.setDouble(10, Config.DROP_ADENA);
                            updateStatement.setDouble(11, Config.DROP_ITEMS);
                            updateStatement.setDouble(12, Config.DROP_SEAL_STONES);
                            updateStatement.setDouble(13, Config.DROP_RAID_ITEMS);
                            updateStatement.setDouble(14, Config.DROP_SPOIL);
                        }

                        updateStatement.executeUpdate();
                    }
                } else {
                    throw new SQLException("Character ID not found.");
                }
            }
        }
    }




    public int getObjectIdByCharName(String charName) throws SQLException {
        String query = "SELECT `obj_Id` FROM `characters` WHERE `char_name` = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, charName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("obj_Id");
                }
            }
        }

        return 0;
    }

    public List<Integer> getStageIdsByCharId(int charId) throws SQLException {
        List<Integer> stageIds = new ArrayList<>();
        String query = "SELECT stageId FROM `progress` WHERE `objId` = ?"; // Заменили objId на charId

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, charId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stageIds.add(resultSet.getInt("stageId"));
                }
            }
        }

        return stageIds;
    }
}