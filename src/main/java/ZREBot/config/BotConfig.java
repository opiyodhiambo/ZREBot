package ZREBot.config;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BotConfig {

    private static final String MEMBER_ROLE_ID = "1099474082422063145";
    private static final String WELCOME_CHANNEL_ID = "1099483814377562192";
    private static final String NAME_LOG_CHANNEL = "1099712957014876181";
    private static final String EVENT_NAME_CHANNEL = "1133911285596176454";
    private static final String MOD_LOG_CHANNEL = "1283561989607784459";

    private static final List<String> AUTO_REACTION_CHANNELS = Arrays.asList(
            "1269416717994426528",
            "1261562170190332004",
            "1197710900426190910"
    );

    private static final String ZRE_EMOJI_NAME = "ZRE";
    private static final String ZRE_EMOJI_ID = "1075937292675461270";

    private static final List<String> MOD_ROLES = Arrays.asList(
            "709747039562366977", "898223053832601611",
            "1284891611922432010", "1329555577646354483",
            "977646240789577849", "1162105401366556732"
    );
    private static final String STAFF_STRIKES_ROLE_ID = "905447795601842206"; // Event Host role

    public String getToken() {
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            token = System.getenv("TOKEN");
        }

        if (token != null && !token.isEmpty()) {
            System.out.println("Found token in environment variables");
            return token;
        }

        try {
            if (Files.exists(Paths.get("discloud.config"))) {
                String config = Files.readString(Paths.get("discloud.config"));
                for (String line : config.split("\n")) {
                    if (line.startsWith("BOT_TOKEN=") || line.startsWith("TOKEN=")) {
                        token = line.substring(line.indexOf('=') + 1).trim();
                        System.out.println("Found token in discloud.config");
                        return token;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading discloud.config: " + e.getMessage());
        }

        List<String> directories = new ArrayList<>();
        directories.add(".");
        directories.add("./src/main/java/ZREBot");
        directories.add("./src/main/java/zrebot");
        directories.add("./src/main/resources");

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        for (String dir : directories) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMissing()
                        .load();

                token = dotenv.get("BOT_TOKEN");
                if (token == null || token.isEmpty()) {
                    token = dotenv.get("TOKEN");
                }

                if (token != null && !token.isEmpty()) {
                    System.out.println("Found token in .env file in " + dir);
                    return token;
                }
            } catch (Exception e) {
                System.out.println("Error loading .env from " + dir + ": " + e.getMessage());
            }
        }

        return token;
    }

    public String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        try {
            if (Files.exists(Paths.get("discloud.config"))) {
                String config = Files.readString(Paths.get("discloud.config"));
                for (String line : config.split("\n")) {
                    if (line.startsWith(key + "=")) {
                        return line.substring(line.indexOf('=') + 1).trim();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading discloud.config for " + key + ": " + e.getMessage());
        }

        return defaultValue;
    }

    public String getStatusText() {
        return getEnvOrDefault("BOT_STATUS", "🌍 Watching ZRE!");
    }

    public String getStatusUrl() {
        return getEnvOrDefault("BOT_STATUS_URL", "https://www.twitch.tv/mrjawesomeyt");
    }

    public OnlineStatus getOnlineStatus() {
        String statusTypeStr = getEnvOrDefault("BOT_ONLINE_STATUS", "ONLINE");
        return OnlineStatus.valueOf(statusTypeStr);
    }

    public String getMemberRoleId() {
        return MEMBER_ROLE_ID;
    }

    public String getWelcomeChannelId() {
        return WELCOME_CHANNEL_ID;
    }

    public String getNameLogChannelId() {
        return NAME_LOG_CHANNEL;
    }

    public String getEventNameChannelId() {
        return EVENT_NAME_CHANNEL;
    }

    public String getModLogChannelId() {
        return MOD_LOG_CHANNEL;
    }

    public List<String> getAutoReactionChannels() {
        return AUTO_REACTION_CHANNELS;
    }

    public String getZreEmojiName() {
        return ZRE_EMOJI_NAME;
    }

    public String getZreEmojiId() {
        return ZRE_EMOJI_ID;
    }

    public List<String> getModRoles() {
        return MOD_ROLES;
    }
    public String getStaffStrikesRoleId() {
        return STAFF_STRIKES_ROLE_ID;
    }
}