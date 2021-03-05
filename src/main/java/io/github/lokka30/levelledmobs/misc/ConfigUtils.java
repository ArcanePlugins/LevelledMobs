package io.github.lokka30.levelledmobs.misc;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.*;

public class ConfigUtils {

    public static int SETTINGS_CREEPER_MAX_RADIUS;
    public static int SETTINGS_SPAWN_DISTANCE_FROM_PLAYER;

    private final LevelledMobs main;

    public ConfigUtils(final LevelledMobs main) {
        this.main = main;
    }

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

    /**
     * This method is used to check if the mob's nametag contains health placeholders.
     * This is used by EntityDamageListener and EntityRegainHealthListener to check if the mob's nametag
     * needs to be updated or not in those events since otherwise the health placeholders may display incorrect values
     * in the mob's nametag.
     *
     * @param livingEntity the entity to check for.
     * @return if the mob's nametag does not contain health placeholders.
     */
    public boolean nametagNotContainsHealthPlaceholders(LivingEntity livingEntity) {
        final String nametag = main.levelManager.getNametag(livingEntity, false);
        return !nametag.contains("%health%") && !nametag.contains("%max_health%") && !nametag.contains("%health_rounded%") && !nametag.contains("%max_health_rounded%");
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
