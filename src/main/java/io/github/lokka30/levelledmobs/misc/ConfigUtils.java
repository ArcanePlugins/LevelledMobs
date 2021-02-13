package io.github.lokka30.levelledmobs.misc;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;

public class ConfigUtils {

    public static int SETTINGS_CREEPER_MAX_RADIUS;
    public static int SETTINGS_SPAWN_DISTANCE_FROM_PLAYER;

    private final LevelledMobs main;

    public ConfigUtils(final LevelledMobs main) {
        this.main = main;
    }

    public void init() {
        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) SETTINGS_CREEPER_MAX_RADIUS = 3;
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1;
    }

    public int getMinLevel(final EntityType entityType, final World world, final boolean isAdult, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {

        // Note for users wondering why '-1' is stored in the min level map:
        // -1 is supposed to be an impossible level to achieve. It is used
        // so the plugin knows that the mob isn't overriden in the settings.
        // This allows the plugin to not have to check the disk every time
        // a mob spawns.

        final String entityTypeStr = entityType.toString();
        final String worldName = world.getName();
        int minLevel = main.settingsCfg.getInt("fine-tuning.min-level", 1);

        if (main.settingsCfg.getBoolean("world-level-override.enabled") && main.worldLevelOverride_Min.containsKey(worldName)) {
            minLevel = Utils.getDefaultIfNull(main.worldLevelOverride_Min, worldName, minLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.WORLD;
                debugInfo.minLevel = minLevel;
            }
            return minLevel;
        }

        if (main.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && main.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
                minLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Min, entityTypeStr + "_REINFORCEMENTS", minLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (isAdult && main.entityTypesLevelOverride_Min.containsKey(entityTypeStr)) {
                minLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Min, entityTypeStr, minLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (!isAdult && main.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr)) {
                minLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, minLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            }
        }

        if (debugInfo != null) debugInfo.minLevel = minLevel;

        return minLevel;
    }

    public int getMaxLevel(final EntityType entityType, final World world, final boolean isAdult, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {

        // Note for users wondering why '-1' is stored in the max level map:
        // -1 is supposed to be an impossible level to achieve. It is used
        // so the plugin knows that the mob isn't overriden in the settings.
        // This allows the plugin to not have to check the disk every time
        // a mob spawns.

        final String entityTypeStr = entityType.toString();
        final String worldName = world.getName();
        int maxLevel = Utils.getDefaultIfNull(main.settingsCfg, "fine-tuning.max-level", 10);

        if (main.settingsCfg.getBoolean("world-level-override.enabled") && main.worldLevelOverride_Max.containsKey(worldName)) {
            maxLevel = Utils.getDefaultIfNull(main.worldLevelOverride_Max, worldName, maxLevel);
            if (debugInfo != null) {
                debugInfo.rule = MobProcessReason.WORLD;
                debugInfo.maxLevel = maxLevel;
            }
            return maxLevel;
        }

        if (main.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && main.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
                maxLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Max, entityTypeStr + "_REINFORCEMENTS", maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (isAdult && main.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
                maxLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Max, entityTypeStr, maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (!isAdult && main.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
                maxLevel = Utils.getDefaultIfNull(main.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            }
        }

        if (debugInfo != null) debugInfo.maxLevel = maxLevel;

        return maxLevel;
    }

    public String getPrefix() {
        return MicroUtils.colorize(Objects.requireNonNull(main.messagesCfg.getString("common.prefix")));
    }

    public void sendNoPermissionMsg(CommandSender sender) {
        List<String> noPermissionMsg = main.messagesCfg.getStringList("common.no-permission");

        noPermissionMsg = Utils.replaceAllInList(noPermissionMsg, "%prefix%", getPrefix());
        noPermissionMsg = Utils.colorizeAllInList(noPermissionMsg);

        noPermissionMsg.forEach(sender::sendMessage);
    }

    public boolean nametagContainsHealth() {
        final String creatureNametag = main.settingsCfg.getString("creature-nametag");

        assert creatureNametag != null;

        return creatureNametag.contains("%health%") || creatureNametag.contains("%max_health%");
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
