package pl.d4nte.bans.data;

import java.sql.*;
import java.util.function.Consumer;

public class Database {

    private final String url;
    private final String user;
    private final String pass;

    private Connection connection;

    public Database(String host, int port, String database, String user, String password, boolean useSSL) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8"
                + "&useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        this.user = user;
        this.pass = password;
    }

    public void init() throws SQLException {
        this.connection = DriverManager.getConnection(url, user, pass);
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS d4nte_bans (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(16) NOT NULL," +
                    "ip VARCHAR(45) NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "staff VARCHAR(16) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "expires_at BIGINT NULL," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "INDEX (uuid), INDEX (name), INDEX (ip))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS d4nte_mutes (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(16) NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "staff VARCHAR(16) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "expires_at BIGINT NOT NULL," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "INDEX (uuid), INDEX (name))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS d4nte_kicks (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(16) NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "staff VARCHAR(16) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "ip VARCHAR(45) NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS d4nte_jails (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "world VARCHAR(64) NOT NULL," +
                    "x DOUBLE NOT NULL,y DOUBLE NOT NULL,z DOUBLE NOT NULL," +
                    "yaw FLOAT NOT NULL,pitch FLOAT NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS d4nte_jailings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(16) NOT NULL," +
                    "jail_id VARCHAR(64) NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "staff VARCHAR(16) NOT NULL," +
                    "created_at BIGINT NOT NULL," +
                    "expires_at BIGINT NOT NULL," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "INDEX (uuid), INDEX (name)," +
                    "FOREIGN KEY (jail_id) REFERENCES d4nte_jails(id) ON DELETE CASCADE)");
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection(url, user, pass);
        }
        return this.connection;
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException ignored) {}
    }

    public void withPrepared(String sql, Consumer<PreparedStatement> consumer) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            consumer.accept(ps);
            ps.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}