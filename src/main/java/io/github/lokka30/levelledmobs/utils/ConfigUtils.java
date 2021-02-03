package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MicroUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigUtils {
    private final Map<EntityType, String> entityNameMap;

    public static int SETTINGS_CREEPER_MAX_RADIUS;
    public static int SETTINGS_SPAWN_DISTANCE_FROM_PLAYER;

    private final LevelledMobs instance;

    public ConfigUtils(final LevelledMobs instance) {
        this.instance = instance;

        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) SETTINGS_CREEPER_MAX_RADIUS = 3;
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1;

        entityNameMap = new HashMap<>();
    }

    public String getEntityName(final EntityType entityType) {
        if (entityNameMap.containsKey(entityType)) {
            return entityNameMap.get(entityType);
        } else {
            String name;
            final String entityTypeStr = entityType.toString();
            final String path = "entity-name-override." + entityTypeStr;

            if (instance.settingsCfg.contains(path)) {
                name = instance.settingsCfg.getString(path);
            } else {
                name = WordUtils.capitalizeFully(entityTypeStr.toLowerCase().replaceAll("_", " "));
            }

            entityNameMap.put(entityType, name);
            return name;
        }
    }

    public int getMinLevel(final EntityType entityType, final World world, final boolean isAdult, final DebugInfo debugInfo, final CreatureSpawnEvent.SpawnReason spawnReason) {

        // Note for users wondering why '-1' is stored in the min level map:
        // -1 is supposed to be an impossible level to achieve. It is used
        // so the plugin knows that the mob isn't overriden in the settings.
        // This allows the plugin to not have to check the disk every time
        // a mob spawns.

        final String entityTypeStr = entityType.toString();
        final String worldName = world.getName();
        int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level", 1);

        if (instance.settingsCfg.getBoolean("world-level-override.enabled") && instance.worldLevelOverride_Min.containsKey(worldName)) {
            minLevel = Utils.getDefaultIfNull(instance.worldLevelOverride_Min, worldName, minLevel);
            if (debugInfo != null){
                debugInfo.rule = MobProcessReason.WORLD;
                debugInfo.minLevel = minLevel;
            }
            return minLevel;
        }

        if (instance.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")){
                minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, entityTypeStr + "_REINFORCEMENTS", minLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (isAdult && instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr)) {
                minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, entityTypeStr, minLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (!isAdult && instance.entityTypesLevelOverride_Min.containsKey("baby_" + entityTypeStr)) {
                minLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Min, "baby_" + entityTypeStr, minLevel);
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
        int maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.max-level", 10);

        if (instance.settingsCfg.getBoolean("world-level-override.enabled") && instance.worldLevelOverride_Max.containsKey(worldName)) {
            maxLevel = Utils.getDefaultIfNull(instance.worldLevelOverride_Max, worldName, maxLevel);
            if (debugInfo != null){
                debugInfo.rule = MobProcessReason.WORLD;
                debugInfo.maxLevel = maxLevel;
            }
            return maxLevel;
        }

        if (instance.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            if (spawnReason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS && instance.entityTypesLevelOverride_Min.containsKey(entityTypeStr + "_REINFORCEMENTS")) {
                maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, entityTypeStr + "_REINFORCEMENTS", maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (isAdult && instance.entityTypesLevelOverride_Max.containsKey(entityTypeStr)) {
                maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, entityTypeStr, maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            } else if (!isAdult && instance.entityTypesLevelOverride_Max.containsKey("baby_" + entityTypeStr)) {
                maxLevel = Utils.getDefaultIfNull(instance.entityTypesLevelOverride_Max, "baby_" + entityTypeStr, maxLevel);
                if (debugInfo != null) debugInfo.rule = MobProcessReason.ENTITY;
            }
        }

        if (debugInfo != null) debugInfo.maxLevel = maxLevel;

        return maxLevel;
    }

    public String getPrefix() {
        return MicroUtils.colorize(Objects.requireNonNull(instance.messagesCfg.getString("common.prefix")));
    }

    public void sendNoPermissionMsg(CommandSender sender) {
        List<String> noPermissionMsg = instance.messagesCfg.getStringList("common.no-permission");

        noPermissionMsg = Utils.replaceAllInList(noPermissionMsg, "%prefix%", getPrefix());
        noPermissionMsg = Utils.colorizeAllInList(noPermissionMsg);

        noPermissionMsg.forEach(sender::sendMessage);
    }

    public boolean nametagContainsHealth() {
        final String creatureNametag = instance.settingsCfg.getString("creature-nametag");

        assert creatureNametag != null;

        return creatureNametag.contains("%health%") || creatureNametag.contains("%max_health%");
    }

}
