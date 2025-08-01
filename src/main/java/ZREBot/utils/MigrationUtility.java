package ZREBot.utils;

import ZREBot.database.DatabaseManager;
import ZREBot.repositories.EventNameRepository;
import ZREBot.repositories.PostgresEventNameRepository;
import ZREBot.models.EventNameData;

import java.util.Map;
import java.util.Scanner;

public class MigrationUtility {

    public static void main(String[] args) {
        System.out.println("=== ZREBot Data Migration Utility ===");
        System.out.println("This utility will migrate your event names from the local file system to PostgreSQL.");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        // Test database connection first
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.testConnection()) {
            System.err.println("❌ Failed to connect to PostgreSQL database!");
            System.err.println("Please check your database configuration and try again.");
            return;
        }

        System.out.println("✅ Database connection successful!");
        System.out.println();

        try {
            // Load old data
            EventNameRepository oldRepo = new EventNameRepository();
            Map<String, EventNameData> oldData = oldRepo.getAllEventNames();

            if (oldData.isEmpty()) {
                System.out.println("ℹ️ No data found in the old file-based system.");
                System.out.println("Nothing to migrate.");
                return;
            }

            System.out.println("📊 Found " + oldData.size() + " event names in the old system:");
            System.out.println();

            // Show sample data
            int count = 0;
            for (Map.Entry<String, EventNameData> entry : oldData.entrySet()) {
                if (count < 5) {
                    EventNameData data = entry.getValue();
                    System.out.println("  - User ID: " + data.getUserId() +
                            ", Event Name: " + data.getName() +
                            ", Date: " + data.getFormattedDate());
                }
                count++;
            }

            if (oldData.size() > 5) {
                System.out.println("  ... and " + (oldData.size() - 5) + " more entries");
            }
            System.out.println();

            // Check if new database has data
            PostgresEventNameRepository newRepo = new PostgresEventNameRepository();
            Map<String, EventNameData> newData = newRepo.getAllEventNames();

            if (!newData.isEmpty()) {
                System.out.println("⚠️ Warning: The PostgreSQL database already contains " + newData.size() + " event names.");
                System.out.print("Do you want to proceed with migration? Existing data will be preserved. (y/N): ");

                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    System.out.println("Migration cancelled.");
                    return;
                }
            } else {
                System.out.print("Proceed with migration? (y/N): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    System.out.println("Migration cancelled.");
                    return;
                }
            }

            System.out.println();
            System.out.println("🔄 Starting migration...");

            // Perform migration
            newRepo.migrateFromOldRepository(oldRepo);

            // Verify migration
            Map<String, EventNameData> migratedData = newRepo.getAllEventNames();
            System.out.println();
            System.out.println("✅ Migration completed!");
            System.out.println("📊 Total entries in PostgreSQL database: " + migratedData.size());

            // Show statistics
            newRepo.printStatistics();

            System.out.println();
            System.out.println("🎉 Migration successful! Your bot can now use PostgreSQL.");
            System.out.println("💡 Tip: You can safely delete the old 'event_names.dat' file after confirming everything works correctly.");

        } catch (Exception e) {
            System.err.println("❌ Migration failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            dbManager.close();
        }
    }

    // Method to verify data integrity after migration
    public static void verifyMigration() {
        System.out.println("=== Verifying Migration ===");

        try {
            EventNameRepository oldRepo = new EventNameRepository();
            PostgresEventNameRepository newRepo = new PostgresEventNameRepository();

            Map<String, EventNameData> oldData = oldRepo.getAllEventNames();
            Map<String, EventNameData> newData = newRepo.getAllEventNames();

            System.out.println("Old system: " + oldData.size() + " entries");
            System.out.println("New system: " + newData.size() + " entries");

            int matchCount = 0;
            int mismatchCount = 0;

            for (Map.Entry<String, EventNameData> entry : oldData.entrySet()) {
                String userId = entry.getKey();
                EventNameData oldEventData = entry.getValue();
                EventNameData newEventData = newData.get(userId);

                if (newEventData != null &&
                        oldEventData.getName().equals(newEventData.getName())) {
                    matchCount++;
                } else {
                    mismatchCount++;
                    System.out.println("❌ Mismatch for user " + userId +
                            ": old='" + oldEventData.getName() +
                            "', new='" + (newEventData != null ? newEventData.getName() : "NOT FOUND") + "'");
                }
            }

            System.out.println();
            System.out.println("✅ Matches: " + matchCount);
            System.out.println("❌ Mismatches: " + mismatchCount);

            if (mismatchCount == 0) {
                System.out.println("🎉 Migration verification successful! All data matches.");
            } else {
                System.out.println("⚠️ Some data mismatches found. Please review the migration.");
            }

        } catch (Exception e) {
            System.err.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}