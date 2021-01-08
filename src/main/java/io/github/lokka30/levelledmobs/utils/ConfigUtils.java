package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class ConfigUtils {
    private final Map<EntityType, String> entityNameMap;
    private final Map<EntityType, Integer> entityTypeMinLevelMap;
    private final Map<EntityType, Integer> entityTypeMaxLevelMap;
    private final Map<String, Integer> worldMinLevelMap;
    private final Map<String, Integer> worldMaxLevelMap;

    public static int SETTINGS_CREEPER_MAX_RADIUS;
    public static int SETTINGS_SPAWN_DISTANCE_FROM_PLAYER;

    private final LevelledMobs instance;

    public ConfigUtils(final LevelledMobs instance) {
        this.instance = instance;

        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) SETTINGS_CREEPER_MAX_RADIUS = 3;
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1;

        entityNameMap = new HashMap<>();
        entityTypeMinLevelMap = new HashMap<>();
        entityTypeMaxLevelMap = new HashMap<>();
        worldMinLevelMap = new HashMap<>();
        worldMaxLevelMap = new HashMap<>();
    }

    public String getEntityName(EntityType entityType) {
        if (entityNameMap.containsKey(entityType)) {
            return entityNameMap.get(entityType);
        } else {
            String name;
            String entityTypeStr = entityType.toString();
            String path = "entity-name-override." + entityTypeStr;

            if (instance.settingsCfg.contains(path)) {
                name = instance.settingsCfg.getString(path);
            } else {
                name = WordUtils.capitalizeFully(entityTypeStr.toLowerCase().replaceAll("_", " "));
            }

            entityNameMap.put(entityType, name);
            return name;
        }
    }

    public int getMinLevel(LivingEntity livingEntity) {

        // Note for users wondering why '-1' is stored in the min level map:
        // -1 is supposed to be an impossible level to achieve. It is used
        // so the plugin knows that the mob isn't overriden in the settings.
        // This allows the plugin to not have to check the disk every time
        // a mob spawns.


        EntityType entityType = livingEntity.getType();
        String entityTypeStr = entityType.toString();

        String worldOverridePath = "world-level-override.min-level." + livingEntity.getWorld().getName();
        String entityTypeOverridePath = "entitytype-level-override.min-level." + entityTypeStr;
        String worldName = livingEntity.getWorld().getName();

        int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level");

        if (instance.settingsCfg.getBoolean("world-level-override.enabled")) {
            if (worldMinLevelMap.containsKey(worldName) && worldMinLevelMap.get(worldName) != -1) {
                return worldMinLevelMap.get(worldName);
            } else {
                if (instance.settingsCfg.contains(worldOverridePath)) {
                    minLevel = Utils.getDefaultIfNull(instance.settingsCfg, worldOverridePath, 1);
                    worldMinLevelMap.put(worldName, minLevel);
                    return minLevel;
                } else {
                    worldMinLevelMap.put(worldName, -1);
                }
            }
        }

        if (instance.settingsCfg.getBoolean("entitytype-level-override.enabled")) {
            if (entityTypeMinLevelMap.containsKey(entityType) && entityTypeMinLevelMap.get(entityType) != -1) {
                return entityTypeMinLevelMap.get(entityType);
            } else {
                if (instance.settingsCfg.contains(entityTypeOverridePath)) {
                    minLevel = Utils.getDefaultIfNull(instance.settingsCfg, entityTypeOverridePath, 1);
                    entityTypeMinLevelMap.put(entityType, minLevel);
                    return minLevel;
                } else {
                    entityTypeMinLevelMap.put(entityType, -1);
                }
            }
        }

        return minLevel;
    }

    public int getMaxLevel(LivingEntity livingEntity) {

        // Note for users wondering why '-1' is stored in the max level map:
        // -1 is supposed to be an impossible level to achieve. It is used
        // so the plugin knows that the mob isn't overriden in the settings.
        // This allows the plugin to not have to check the disk every time
        // a mob spawns.


        EntityType entityType = livingEntity.getType();
        String entityTypeStr = entityType.toString();

        String worldOverridePath = "world-level-override.max-level." + livingEntity.getWorld().getName();
        String entityTypeOverridePath = "entitytype-level-override.max-level." + entityTypeStr;
        String worldName = livingEntity.getWorld().getName();

        int maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, "fine-tuning.max-level", 10);

        if (instance.settingsCfg.getBoolean("world-level-override.enabled")) {
            if (worldMaxLevelMap.containsKey(worldName) && worldMaxLevelMap.get(worldName) != -1) {
                return worldMaxLevelMap.get(worldName);
            } else {
                if (instance.settingsCfg.contains(worldOverridePath)) {
                    maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, worldOverridePath, 10);
                    worldMaxLevelMap.put(worldName, maxLevel);
                    return maxLevel;
                } else {
                    worldMaxLevelMap.put(worldName, -1);
                }
            }
        }

        if (instance.settingsCfg.getBoolean("entity-type-level-override.enabled")) {
            if (entityTypeMaxLevelMap.containsKey(entityType) && entityTypeMaxLevelMap.get(entityType) != -1) {
                return entityTypeMaxLevelMap.get(entityType);
            } else {
                if (instance.settingsCfg.contains(entityTypeOverridePath)) {
                    maxLevel = Utils.getDefaultIfNull(instance.settingsCfg, entityTypeOverridePath, 10);
                    entityTypeMaxLevelMap.put(entityType, maxLevel);
                    return maxLevel;
                } else {
                    entityTypeMaxLevelMap.put(entityType, -1);
                }
            }
        }

        return maxLevel;
    }

}
