/*
 * Copyright (c) 2023. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package dev.se1dhe.bot.service.dbManager;

import java.sql.*;

public class EternityManager extends Manager{

    public EternityManager(String url, String username, String password) throws SQLException {
        super(url, username, password);
    }

    @Override
    protected Connection getConnection(String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void addItem(int ownerId, int itemId, int count) throws SQLException {
        String query = "INSERT INTO `items_delayed` (`payment_id`,`owner_id`, `item_id`, `count`, `enchant_level`, `payment_status`, `description`) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, 0);
            statement.setInt(2, ownerId);
            statement.setInt(3, itemId);
            statement.setInt(4, count);
            statement.setInt(5, 0);
            statement.setInt(6, 0);
            statement.setInt(7, 0);


            statement.executeUpdate();
        }
    }

    public void addPremiumData(int charId, int expireTimeHours) throws SQLException {
        String selectQuery = "SELECT `expireTime`, `status`, `id` FROM `character_premium_personal` WHERE `charId` = ?";
        String insertQuery = "INSERT INTO `character_premium_personal` (`charId`, `id`, `status`, `expireTime`) VALUES (?, ?, ?, ?)";
        String updateQuery = "UPDATE `character_premium_personal` SET `expireTime` = ?, `status` = ?, `id` = ? WHERE `charId` = ?";
        long expireTimeMillis = (long) expireTimeHours * 60 * 60 * 1000;

        try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
            selectStatement.setInt(1, charId);
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                long currentExpireTime = resultSet.getLong("expireTime");
                if (currentExpireTime == 0) {
                    currentExpireTime = System.currentTimeMillis();
                }

                long newExpireTime = currentExpireTime + expireTimeMillis;

                int currentStatus = resultSet.getInt("status");
                int newStatus = (currentStatus == 0) ? 1 : currentStatus;

                int currentId = resultSet.getInt("id");
                int newId = (currentId == 0) ? 1 : currentId;

                try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                    updateStatement.setLong(1, newExpireTime);
                    updateStatement.setInt(2, newStatus);
                    updateStatement.setInt(3, newId);
                    updateStatement.setInt(4, charId);

                    updateStatement.executeUpdate();
                }
            } else {
                long currentTimeMillis = System.currentTimeMillis();
                long expireTime = currentTimeMillis + expireTimeMillis;

                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    insertStatement.setInt(1, charId);
                    insertStatement.setInt(2, 1);
                    insertStatement.setInt(3, 1);
                    insertStatement.setLong(4, expireTime);

                    insertStatement.executeUpdate();
                }
            }
        }
    }







    public int getObjectIdByCharName(String charName) throws SQLException {
        String query = "SELECT `charId` FROM `characters` WHERE `char_name` = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, charName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("charId");
                }
            }
        }

        return 0;
    }
}
