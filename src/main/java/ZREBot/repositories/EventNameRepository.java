package ZREBot.repositories;

import ZREBot.models.EventNameData;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class EventNameRepository {
    private static final Map<String, EventNameData> eventNames = new ConcurrentHashMap<>();
    private static final String EVENT_NAMES_FILE = "event_names.dat";

    public EventNameRepository() {
        deleteAllBackupFiles();
        loadEventNames();
        setupAutoSave();
    }

    private void deleteAllBackupFiles() {
        try {
            File dir = new File(".");
            File[] backupFiles = dir.listFiles((d, name) ->
                    name.startsWith(EVENT_NAMES_FILE) &&
                            (name.endsWith(".tmp") || name.contains(".bak") || name.endsWith(".backup")));

            if (backupFiles != null) {
                for (File file : backupFiles) {
                    file.delete();
                    System.out.println("Deleted backup file: " + file.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up backup files: " + e.getMessage());
        }
    }

    private void debugEventNameSystem(String operation) {
        System.out.println("\n----- EVENT NAME SYSTEM DEBUG (" + operation + ") -----");
        System.out.println("Time: " + new Date());
        System.out.println("Total users with event names: " + eventNames.size());

        System.out.println("\nDETAILED USER EVENT NAMES:");
        if (eventNames.isEmpty()) {
            System.out.println("  No users have any event names.");
        } else {
            for (Map.Entry<String, EventNameData> entry : eventNames.entrySet()) {
                String userId = entry.getKey();
                EventNameData data = entry.getValue();

                System.out.println("\n  USER ID: " + userId);
                System.out.println("  - Event Name: " + data.getName());
                System.out.println("  - Submitted: " + data.getFormattedDate());
            }
        }

        File file = new File(EVENT_NAMES_FILE);
        System.out.println("\nFILE STATUS:");
        System.out.println("  - File exists: " + file.exists());
        if (file.exists()) {
            System.out.println("  - File size: " + file.length() + " bytes");
            System.out.println("  - Last modified: " + new Date(file.lastModified()));
        }
        System.out.println("----- END DEBUG -----\n");
    }

    private void setupAutoSave() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Auto-saving event names data...");
                saveEventNames();
            }
        }, 5 * 60 * 1000, 5 * 60 * 1000);
    }

    public void saveEventNames() {
        debugEventNameSystem("BEFORE SAVE");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(EVENT_NAMES_FILE))) {
            oos.writeObject(eventNames);
            System.out.println("Event names data saved successfully. Total users with event names: " + eventNames.size());
        } catch (IOException e) {
            System.err.println("Error saving event names data: " + e.getMessage());
            e.printStackTrace();
        }

        debugEventNameSystem("AFTER SAVE");
    }

    @SuppressWarnings("unchecked")
    private void loadEventNames() {
        System.out.println("Starting to load event names...");

        File file = new File(EVENT_NAMES_FILE);
        if (!file.exists()) {
            System.out.println("No saved event names file found, starting fresh");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, EventNameData> loadedEventNames = (Map<String, EventNameData>) ois.readObject();

            eventNames.clear();
            eventNames.putAll(loadedEventNames);

            System.out.println("Loaded event names for " + eventNames.size() + " users");

            debugEventNameSystem("AFTER LOADING");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading event names data: " + e.getMessage());
            System.out.println("If this is your first run with the new system, this error is expected and can be ignored.");
        }
    }

    public void saveEventName(String userId, String name) {
        EventNameData data = new EventNameData(userId, name.toLowerCase(), System.currentTimeMillis());
        eventNames.put(userId, data);
        saveEventNames();
    }

    public EventNameData getEventNameByUser(String userId) {
        return eventNames.get(userId);
    }

    public List<EventNameData> searchEventNameByName(String name) {
        List<EventNameData> results = new ArrayList<>();
        String nameLower = name.toLowerCase();

        for (EventNameData data : eventNames.values()) {
            if (data.getName().contains(nameLower)) {
                results.add(data);
            }
        }

        return results;
    }

    public EventNameData getEventNameByUserAndName(String userId, String name) {
        EventNameData data = eventNames.get(userId);
        if (data != null && data.getName().equalsIgnoreCase(name)) {
            return data;
        }
        return null;
    }

    public Map<String, EventNameData> getAllEventNames() {
        return new HashMap<>(eventNames);
    }
}