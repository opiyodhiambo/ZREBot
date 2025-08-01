package ZREBot.utils;

import ZREBot.config.BotConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;

public class PermissionUtils {

    public static boolean isModerator(Member member, BotConfig config) {

        if (member.hasPermission(Permission.BAN_MEMBERS)) {
            return true;
        }

        List<String> modRoles = config.getModRoles();
        return member.getRoles().stream()
                .anyMatch(role -> modRoles.contains(role.getId()));
    }

    public static boolean isAdmin(Member member) {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }
}