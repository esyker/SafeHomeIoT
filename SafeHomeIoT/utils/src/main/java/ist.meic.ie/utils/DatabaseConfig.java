package ist.meic.ie.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private Connection connection;

    public DatabaseConfig(String ip, String dbName, String masterUsername, String password) {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + ip + ":3306/" + dbName, masterUsername, password);

        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
