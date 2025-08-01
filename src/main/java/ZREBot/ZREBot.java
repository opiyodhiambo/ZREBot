package ZREBot;

import ZREBot.config.BotConfig;
import ZREBot.database.DatabaseManager;
import ZREBot.events.*;
import ZREBot.repositories.EventNameRepository;
import ZREBot.repositories.PostgresEventNameRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class ZREBot {
    private JDA jda;
    private BotConfig config;
    private PostgresEventNameRepository eventNameRepository;
    private EventNameRepository oldEventNameRepository;

    public ZREBot() {
        this.config = new BotConfig();
    }

    public void start() {
        try {
            System.out.println("Starting ZREBot...");

            String token = config.getToken();
            if (token == null || token.isEmpty()) {
                System.err.println("Bot token not found! Please set BOT_TOKEN in environment variables or .env file.");
                return;
            }

            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.testConnection()) {
                System.err.println("Failed to connect to database! Please check your database configuration.");
                return;
            }
            System.out.println("Database connection successful!");

            this.eventNameRepository = new PostgresEventNameRepository();

            this.oldEventNameRepository = new EventNameRepository();
            migrateOldDataIfNeeded();

            this.jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.streaming(config.getStatusText(), config.getStatusUrl()))
                    .setStatus(config.getOnlineStatus())
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS
                    )
                    .addEventListeners(
                            new CommandEventListener(this),
                            new GuildEventListener(this),
                            new MessageEventListener(this),
                            new ModalEventListener(this),
                            new ReactionEventListener(this)
                    )
                    .build()
                    .awaitReady();

            CommandEventListener.registerCommands(jda);

            eventNameRepository.printStatistics();

            System.out.println("ZREBot started successfully!");

        } catch (Exception e) {
            System.err.println("Failed to start bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void migrateOldDataIfNeeded() {
        try {

            var oldData = oldEventNameRepository.getAllEventNames();
            var newData = eventNameRepository.getAllEventNames();

            if (!oldData.isEmpty() && newData.isEmpty()) {
                System.out.println("Found old data (" + oldData.size() + " entries). Starting migration...");
                eventNameRepository.migrateFromOldRepository(oldEventNameRepository);
                System.out.println("Migration completed successfully!");
            } else if (!oldData.isEmpty() && !newData.isEmpty()) {
                System.out.println("Both old and new data exist. Skipping automatic migration.");
                System.out.println("Old data: " + oldData.size() + " entries, New data: " + newData.size() + " entries");
            } else {
                System.out.println("No migration needed.");
            }
        } catch (Exception e) {
            System.err.println("Error during migration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public BotConfig getConfig() {
        return config;
    }

    public PostgresEventNameRepository getEventNameRepository() {
        return eventNameRepository;
    }

    @Deprecated
    public EventNameRepository getOldEventNameRepository() {
        return oldEventNameRepository;
    }

    public JDA getJda() {
        return jda;
    }

    public void shutdown() {
        System.out.println("Shutting down ZREBot...");

        if (eventNameRepository != null) {
            eventNameRepository.close();
        }

        if (jda != null) {
            jda.shutdown();
        }

        System.out.println("ZREBot shutdown complete.");
    }

    public static void main(String[] args) {
        ZREBot bot = new ZREBot();

        Runtime.getRuntime().addShutdownHook(new Thread(bot::shutdown));

        bot.start();
    }
}