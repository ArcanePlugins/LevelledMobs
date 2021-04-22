package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * TODO Describe...
 *
 * @author lokka30
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
    public TreeMap<String, Integer> entityTypesLevelOverride_Min;
    public TreeMap<String, Integer> entityTypesLevelOverride_Max;
    public TreeMap<String, Integer> worldLevelOverride_Min;
    public TreeMap<String, Integer> worldLevelOverride_Max;
    public Set<String> noDropMultiplierEntities;
    public Set<String> overridenEntities;

    public void load() {
        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) SETTINGS_CREEPER_MAX_RADIUS = 3;
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1;
    }

    public String getPrefix() {
        return MessageUtils.colorizeAll(Objects.requireNonNull(main.messagesCfg.getString("common.prefix")));
    }

    public void sendNoPermissionMsg(CommandSender sender) {
        List<String> noPermissionMsg = main.messagesCfg.getStringList("common.no-permission");

        noPermissionMsg = Utils.replaceAllInList(noPermissionMsg, "%prefix%", getPrefix());
        noPermissionMsg = Utils.colorizeAllInList(noPermissionMsg);

        noPermissionMsg.forEach(sender::sendMessage);
    }

    public TreeMap<String, Integer> getMapFromConfigSection(final String configPath) {
        final TreeMap<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final ConfigurationSection cs = main.settingsCfg.getConfigurationSection(configPath);
        if (cs == null) return result;

        final Set<String> set = cs.getKeys(false);

        for (final String item : set) {
            final Object value = cs.get(item);
            if (value != null && Utils.isInteger(value.toString())) {
                result.put(item, Integer.parseInt(value.toString()));
            }
        }

        return result;
    }

    public Set<String> getSetFromConfigSection(final String configPath) {
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final List<String> set = main.settingsCfg.getStringList(configPath);

        result.addAll(set);

        return result;
    }

}
