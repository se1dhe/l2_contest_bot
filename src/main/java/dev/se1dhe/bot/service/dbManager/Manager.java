

package dev.se1dhe.bot.service.dbManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class Manager {
    protected final Connection connection;

    public Manager(String url, String username, String password) throws SQLException {
        connection = getConnection(url, username, password);
    }

    protected abstract Connection getConnection(String url, String username, String password) throws SQLException;

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public abstract int getObjectIdByCharName(String charName) throws SQLException;

    public abstract void addItem(int objId, int itemId, int count) throws SQLException;
    public abstract void addPremiumData(int charId, int expireTimeHours) throws SQLException;

}