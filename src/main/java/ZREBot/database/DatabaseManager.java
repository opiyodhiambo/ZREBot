package ZREBot.database;

import ZREBot.config.BotConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    private BotConfig config;

    private DatabaseManager() {
        this.config = new BotConfig();
        setupDataSource();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void setupDataSource() {
        HikariConfig hikariConfig = new HikariConfig();

        String dbUrl = config.getEnvOrDefault("DATABASE_URL", null);
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = config.getEnvOrDefault("DATABASE_PATH", null);
        }
        String dbUser = config.getEnvOrDefault("DATABASE_USER", null);
        String dbPassword = config.getEnvOrDefault("DATABASE_PASSWORD", null);

        if (dbUrl == null || dbUrl.isEmpty()) {
            String host = config.getEnvOrDefault("DATABASE_HOST", null);
            String port = config.getEnvOrDefault("DATABASE_PORT", null);
            String database = config.getEnvOrDefault("DATABASE_NAME", null);

            if (host != null && port != null && database != null) {
                dbUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            }
        }

        System.out.println("DATABASE URL " + dbUrl);

        // If URL contains user and password parameters, extract them
        if (dbUrl != null && (dbUser == null || dbPassword == null)) {
            if (dbUrl.contains("user=") && dbUrl.contains("password=")) {
                // Extract user from URL
                if (dbUser == null) {
                    String userParam = extractUrlParameter(dbUrl, "user");
                    if (userParam != null) {
                        dbUser = userParam;
                    }
                }
                // Extract password from URL
                if (dbPassword == null) {
                    String passwordParam = extractUrlParameter(dbUrl, "password");
                    if (passwordParam != null) {
                        dbPassword = passwordParam;
                    }
                }
            }
        }

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database configuration is missing. Please set DATABASE_URL/DATABASE_PATH or DATABASE_HOST/PORT/NAME, DATABASE_USER, and DATABASE_PASSWORD in environment variables or discloud.config file.");
        }

        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(60000);

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(hikariConfig);

        System.out.println("Database connection pool initialized successfully");
    }

    private String extractUrlParameter(String url, String paramName) {
        String paramPrefix = paramName + "=";
        int startIndex = url.indexOf(paramPrefix);
        if (startIndex == -1) {
            return null;
        }
        startIndex += paramPrefix.length();
        int endIndex = url.indexOf("&", startIndex);
        if (endIndex == -1) {
            endIndex = url.length();
        }
        return url.substring(startIndex, endIndex);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}