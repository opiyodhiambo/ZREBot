package ZREBot.repositories;
import ZREBot.database.DatabaseManager;
import ZREBot.models.EventNameData;

import java.sql.*;
import java.util.*;

public class PostgresEventNameRepository {
    private final DatabaseManager dbManager;

    public PostgresEventNameRepository() {
        this.dbManager = DatabaseManager.getInstance();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = dbManager.getConnection()) {

            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS event_names (
                    id SERIAL PRIMARY KEY,
                    user_id VARCHAR(20) NOT NULL UNIQUE,
                    event_name VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_event_names_user_id ON event_names(user_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_event_names_event_name ON event_names(event_name)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_event_names_created_at ON event_names(created_at)");
            }

            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveEventName(String userId, String eventName) {
        String sql = """
            INSERT INTO event_names (user_id, event_name, updated_at) 
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) 
            DO UPDATE SET 
                event_name = EXCLUDED.event_name,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setString(2, eventName.toLowerCase());

            int rowsAffected = stmt.executeUpdate();
            System.out.println("Event name saved for user " + userId + ": " + eventName +
                    " (rows affected: " + rowsAffected + ")");

        } catch (SQLException e) {
            System.err.println("Error saving event name for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public EventNameData getEventNameByUser(String userId) {
        String sql = "SELECT user_id, event_name, EXTRACT(EPOCH FROM created_at) * 1000 as timestamp FROM event_names WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EventNameData(
                            rs.getString("user_id"),
                            rs.getString("event_name"),
                            rs.getLong("timestamp")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving event name for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public List<EventNameData> searchEventNameByName(String name) {
        String sql = "SELECT user_id, event_name, EXTRACT(EPOCH FROM created_at) * 1000 as timestamp FROM event_names WHERE event_name ILIKE ?";
        List<EventNameData> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + name.toLowerCase() + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new EventNameData(
                            rs.getString("user_id"),
                            rs.getString("event_name"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching event names by name '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    public EventNameData getEventNameByUserAndName(String userId, String name) {
        String sql = "SELECT user_id, event_name, EXTRACT(EPOCH FROM created_at) * 1000 as timestamp FROM event_names WHERE user_id = ? AND event_name ILIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setString(2, name.toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EventNameData(
                            rs.getString("user_id"),
                            rs.getString("event_name"),
                            rs.getLong("timestamp")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving event name for user " + userId + " and name '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public Map<String, EventNameData> getAllEventNames() {
        String sql = "SELECT user_id, event_name, EXTRACT(EPOCH FROM created_at) * 1000 as timestamp FROM event_names";
        Map<String, EventNameData> results = new HashMap<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String userId = rs.getString("user_id");
                results.put(userId, new EventNameData(
                        userId,
                        rs.getString("event_name"),
                        rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all event names: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    public void migrateFromOldRepository(EventNameRepository oldRepository) {
        System.out.println("Starting migration from old repository...");

        Map<String, EventNameData> oldData = oldRepository.getAllEventNames();
        int migratedCount = 0;

        for (Map.Entry<String, EventNameData> entry : oldData.entrySet()) {
            EventNameData data = entry.getValue();

            String sql = """
                INSERT INTO event_names (user_id, event_name, created_at, updated_at) 
                VALUES (?, ?, to_timestamp(?), to_timestamp(?))
                ON CONFLICT (user_id) DO NOTHING
                """;

            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, data.getUserId());
                stmt.setString(2, data.getName());
                stmt.setDouble(3, data.getTimestamp() / 1000.0);
                stmt.setDouble(4, data.getTimestamp() / 1000.0);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    migratedCount++;
                }

            } catch (SQLException e) {
                System.err.println("Error migrating data for user " + data.getUserId() + ": " + e.getMessage());
            }
        }

        System.out.println("Migration completed. Migrated " + migratedCount + " event names out of " + oldData.size() + " total.");
    }

    public void printStatistics() {
        String sql = "SELECT COUNT(*) as total_count, MAX(created_at) as latest_submission FROM event_names";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int totalCount = rs.getInt("total_count");
                Timestamp latestSubmission = rs.getTimestamp("latest_submission");

                System.out.println("=== Event Names Database Statistics ===");
                System.out.println("Total event names stored: " + totalCount);
                System.out.println("Latest submission: " + (latestSubmission != null ? latestSubmission : "None"));
                System.out.println("======================================");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving statistics: " + e.getMessage());
        }
    }

    public void close() {
        dbManager.close();
    }
}