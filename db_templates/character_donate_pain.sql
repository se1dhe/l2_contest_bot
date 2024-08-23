CREATE TABLE IF NOT EXISTS `character_donate` (
	`id` INT(9) UNSIGNED NOT NULL PRIMARY KEY auto_increment,
	`obj_Id` INT(11) NOT NULL DEFAULT 0,
	`char_name` VARCHAR(35) CHARACTER SET UTF8 NOT NULL DEFAULT '',
	`item_id` INT(9) NOT NULL DEFAULT 0,
	`count` INT(11) NOT NULL DEFAULT 1,
	`enchant` INT(5) NOT NULL DEFAULT 0,
	`given` TINYINT NOT NULL DEFAULT 0);
	
	
	import java.sql.*;

public class CharacterDonateDAO {

    private Connection connection;

    public CharacterDonateDAO() {
        try {
            // Replace 'url', 'user', and 'password' with your own database credentials
            String url = "jdbc:mysql://localhost:3306/mydatabase";
            String user = "root";
            String password = "password";

            // Create a new database connection
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setObjId(int id, int objId) {
        try {
            String sql = "UPDATE character_donate SET obj_Id=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, objId);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCharName(int id, String charName) {
        try {
            String sql = "UPDATE character_donate SET char_name=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, charName);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setItemId(int id, int itemId) {
        try {
            String sql = "UPDATE character_donate SET item_id=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, itemId);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setCount(int id, int count) {
        try {
            String sql = "UPDATE character_donate SET count=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, count);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setEnchant(int id, int enchant) {
        try {
            String sql = "UPDATE character_donate SET enchant=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, enchant);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGiven(int id, boolean given) {
        try {
            String sql = "UPDATE character_donate SET given=? WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setBoolean(1, given);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}