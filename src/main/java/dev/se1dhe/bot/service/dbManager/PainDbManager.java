

package dev.se1dhe.bot.service.dbManager;

import java.sql.*;
import java.util.List;

public class PainDbManager extends Manager {
    public PainDbManager(String url, String username, String password) throws SQLException {
        super(url, username, password);
    }

    @Override
    protected Connection getConnection(String url, String username, String password) throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void addItem(int objId,  int itemId, int count) throws SQLException {
        String query = "INSERT INTO `character_donate` (`obj_Id`, `char_name`, `item_id`, `count`, `enchant`, `life_time`, `given`) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, objId);
            statement.setString(2, "");
            statement.setInt(3, itemId);
            statement.setInt(4, count);
            statement.setInt(5, 0); // enchant
            statement.setInt(6, 0); // life_time
            statement.setInt(7, 0); // given

            statement.executeUpdate();
        }
    }

    @Override
    public void addPremiumData(int charId, int expireTimeHours) throws SQLException {
        String selectQuery = "SELECT `bonus_expire` FROM `account_bonus` WHERE `account` = ?";
        String insertQuery = "INSERT INTO `account_bonus` (`account`, `bonus`, `bonus_expire`) VALUES (?, ?, ?)";
        String updateQuery = "UPDATE `account_bonus` SET `bonus` = ?, `bonus_expire` = ? WHERE `account` = ?";
        int expireTimeSeconds = expireTimeHours * 60 * 60;

        try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
            selectStatement.setString(1, String.valueOf(charId));
            ResultSet resultSet = selectStatement.executeQuery();

            if (resultSet.next()) {
                int currentExpire = resultSet.getInt("bonus_expire");
                int newExpire = currentExpire + expireTimeSeconds;

                try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                    updateStatement.setDouble(1, 0.0);
                    updateStatement.setInt(2, newExpire);
                    updateStatement.setString(3, String.valueOf(charId));

                    updateStatement.executeUpdate();
                }
            } else {
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                int expireTime = Math.toIntExact((expireTimeSeconds > 0) ? currentTimeSeconds + expireTimeSeconds : 0);

                try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                    insertStatement.setString(1, String.valueOf(charId));
                    insertStatement.setDouble(2, 0.0);
                    insertStatement.setInt(3, expireTime);

                    insertStatement.executeUpdate();
                }
            }
        }
    }

    @Override
    public List<Integer> getStageIdsByCharId(int charId) throws SQLException {
        return List.of();
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




}