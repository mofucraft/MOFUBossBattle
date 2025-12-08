package net.mofucraft.bossbattle.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.mofucraft.bossbattle.MofuBossBattle;
import net.mofucraft.bossbattle.config.ConfigManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private final MofuBossBattle plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public DatabaseManager(MofuBossBattle plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8",
                    configManager.getDbHost(),
                    configManager.getDbPort(),
                    configManager.getDbDatabase()));
            config.setUsername(configManager.getDbUsername());
            config.setPassword(configManager.getDbPassword());
            config.setMaximumPoolSize(configManager.getDbPoolSize());
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(10000);
            config.setMaxLifetime(600000);
            config.setPoolName("MofuBossBattle-Pool");

            // Performance settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            // Create tables
            createTables();

            plugin.getLogger().info("Database connection established!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
        return dataSource.getConnection();
    }

    private void createTables() {
        String createRankingsTable = """
                CREATE TABLE IF NOT EXISTS boss_rankings (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    boss_id VARCHAR(64) NOT NULL,
                    clear_time_ms BIGINT NOT NULL,
                    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_boss_time (boss_id, clear_time_ms ASC),
                    INDEX idx_player_boss (player_uuid, boss_id),
                    INDEX idx_recorded_at (recorded_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

        String createHistoryTable = """
                CREATE TABLE IF NOT EXISTS battle_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    boss_id VARCHAR(64) NOT NULL,
                    result VARCHAR(16) NOT NULL,
                    duration_ms BIGINT NOT NULL,
                    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_player (player_uuid),
                    INDEX idx_boss (boss_id),
                    INDEX idx_recorded_at (recorded_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRankingsTable);
            stmt.execute(createHistoryTable);
            plugin.getLogger().info("Database tables created/verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
