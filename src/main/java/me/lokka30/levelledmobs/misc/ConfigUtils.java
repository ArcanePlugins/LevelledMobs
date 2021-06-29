package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Used for managing configuration data
 *
 * @author lokka30, stumper66
 */
public class ConfigUtils {

    public static int SETTINGS_CREEPER_MAX_RADIUS;
    public static int SETTINGS_SPAWN_DISTANCE_FROM_PLAYER;

    private final LevelledMobs main;

    public ConfigUtils(final LevelledMobs main) {
        this.main = main;
    }

    public boolean debugEntityDamageWasEnabled = false;
    public boolean chunkLoadListenerWasEnabled = false;

    public void load() {
        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) SETTINGS_CREEPER_MAX_RADIUS = 3;
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1;
    }

    public String getPrefix() {
        return MessageUtils.colorizeAll(Objects.requireNonNull(main.messagesCfg.getString("common.prefix")));
    }

    public void sendNoPermissionMsg(@NotNull final CommandSender sender) {
        List<String> noPermissionMsg = main.messagesCfg.getStringList("common.no-permission");

        noPermissionMsg = Utils.replaceAllInList(noPermissionMsg, "%prefix%", getPrefix());
        noPermissionMsg = Utils.colorizeAllInList(noPermissionMsg);

        noPermissionMsg.forEach(sender::sendMessage);
    }
}
