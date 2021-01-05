package io.github.lokka30.levelledmobs.utils;

import de.leonhard.storage.internal.FlatFile;
import io.github.lokka30.levelledmobs.LevelledMobs;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.*;

@SuppressWarnings("unused")
public class FileCache {

    public boolean SETTINGS_USE_UPDATE_CHECKER;
    public List<String> SETTINGS_BLACKLISTED_REASONS;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_MAX_HEALTH;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_MOVEMENT_SPEED;
    public double SETTINGS_FINE_TUNING_DEFAULT_ATTACK_DAMAGE_INCREASE;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_ATTACK_DAMAGE;
    public int SETTINGS_SPAWN_DISTANCE_LEVELLING_INCREASE_LEVEL_DISTANCE;
    public boolean SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_ENABLED;
    public int SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MIN;
    public int SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MAX;
    public int SETTINGS_SPAWN_DISTANCE_LEVELLING_START_DISTANCE;
    public boolean SETTINGS_DEBUG;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_RANGED_ATTACK_DAMAGE;
    public List<String> SETTINGS_BLACKLIST_OVERRIDE_TYPES;
    public List<String> SETTINGS_BLACKLISTED_TYPES;
    public boolean SETTINGS_LEVEL_PASSIVE;
    public boolean SETTINGS_ENABLE_NAMETAG_CHANGES;
    public boolean SETTINGS_UPDATE_NAMETAG_HEALTH;
    public boolean SETTINGS_FINE_TUNING_CUSTOM_NAME_VISIBLE;
    public boolean SETTINGS_FINE_TUNING_REMOVE_NAMETAG_ON_DEATH;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_ITEM_DROP;
    public double SETTINGS_FINE_TUNING_MULTIPLIERS_XP_DROP;
    public int SETTINGS_FINE_TUNING_MIN_LEVEL;
    public int SETTINGS_FINE_TUNING_MAX_LEVEL;
    public boolean SETTINGS_SPAWN_DISTANCE_LEVELLING_ACTIVE;
    public boolean SETTINGS_WORLDS_LIST_ENABLED;
    public List<String> SETTINGS_WORLDS_LIST_LIST;
    public String SETTINGS_WORLDS_LIST_MODE;
    public boolean SETTINGS_ENTITYTYPE_LEVEL_OVERRIDE_ENABLED;
    public boolean SETTINGS_WORLD_LEVEL_OVERRIDE_ENABLED;
    public boolean SETTINGS_PASSIVE_MOBS_CHANGED_MOVEMENT_SPEED;
    public String SETTINGS_CREATURE_NAMETAG;
    public int SETTINGS_CREEPER_MAX_RADIUS;
    private FlatFile settings;
    private HashMap<EntityType, String> entityNameMap;
    private HashMap<EntityType, Integer> entityTypeMinLevelMap;
    private HashMap<EntityType, Integer> entityTypeMaxLevelMap;
    private HashMap<String, Integer> worldMinLevelMap;
    private HashMap<String, Integer> worldMaxLevelMap;

    public FileCache(final LevelledMobs instance) {
        this.settings = instance.settings;
    }

    public void loadLatest() {
        SETTINGS_USE_UPDATE_CHECKER = settings.get("use-update-checker", true);
        SETTINGS_BLACKLISTED_REASONS = settings.get("blacklisted-reasons", new ArrayList<>());
        SETTINGS_FINE_TUNING_MULTIPLIERS_MAX_HEALTH = settings.get("fine-tuning.multipliers.max-health", 0.2D);
        SETTINGS_FINE_TUNING_MULTIPLIERS_MOVEMENT_SPEED = settings.get("fine-tuning.multipliers.movement-speed", 0.065D);
        SETTINGS_FINE_TUNING_DEFAULT_ATTACK_DAMAGE_INCREASE = settings.get("fine-tuning.default-attack-damage-increase", 1.0D);
        SETTINGS_FINE_TUNING_MULTIPLIERS_ATTACK_DAMAGE = settings.get("fine-tuning.multipliers.attack-damage", 1.5D);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_INCREASE_LEVEL_DISTANCE = settings.get("spawn-distance-levelling.increase-level-distance", 200);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_ENABLED = settings.get("spawn-distance-levelling.variance.enabled", true);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MIN = settings.get("spawn-distance-levelling.variance.min", 0);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_VARIANCE_MAX = settings.get("spawn-distance-levelling.variance.max", 2);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_START_DISTANCE = settings.get("spawn-distance-levelling.start-distance", 0);
        SETTINGS_DEBUG = settings.get("debug", false);
        SETTINGS_FINE_TUNING_MULTIPLIERS_RANGED_ATTACK_DAMAGE = settings.get("fine-tuning.multipliers.ranged-attack-damage", 1.1F);
        SETTINGS_BLACKLIST_OVERRIDE_TYPES = settings.get("blacklist-override-types", Collections.singletonList("SHULKER"));
        SETTINGS_BLACKLISTED_TYPES = settings.get("blacklisted-types", Arrays.asList("VILLAGER", "WANDERING_TRADER", "ENDER_DRAGON", "WITHER"));
        SETTINGS_LEVEL_PASSIVE = settings.get("level-passive", false);
        SETTINGS_ENABLE_NAMETAG_CHANGES = settings.get("enable-nametag-changes", true);
        SETTINGS_UPDATE_NAMETAG_HEALTH = settings.get("update-nametag-health", false);
        SETTINGS_FINE_TUNING_CUSTOM_NAME_VISIBLE = settings.get("fine-tuning.custom-name-visible", false);
        SETTINGS_FINE_TUNING_REMOVE_NAMETAG_ON_DEATH = settings.get("fine-tuning.remove-nametag-on-death", false);
        SETTINGS_FINE_TUNING_MULTIPLIERS_ITEM_DROP = settings.get("fine-tuning.multipliers.item-drop", 0.16);
        SETTINGS_FINE_TUNING_MULTIPLIERS_XP_DROP = settings.get("fine-tuning.multipliers.xp-drop", 0.1D);
        SETTINGS_FINE_TUNING_MIN_LEVEL = settings.get("fine-tuning.min-level", 1);
        SETTINGS_FINE_TUNING_MAX_LEVEL = settings.get("fine-tuning.max-level", 10);
        SETTINGS_SPAWN_DISTANCE_LEVELLING_ACTIVE = settings.get("spawn-distance-levelling.active", false);
        SETTINGS_WORLDS_LIST_ENABLED = settings.get("worlds-list.enabled", false);
        SETTINGS_WORLDS_LIST_LIST = settings.get("worlds-list.list", Collections.singletonList("world"));
        SETTINGS_WORLDS_LIST_MODE = settings.get("worlds-list.mode", "BLACKLIST").toUpperCase();
        SETTINGS_ENTITYTYPE_LEVEL_OVERRIDE_ENABLED = settings.get("entitytype-level-override.enabled", false);
        SETTINGS_WORLD_LEVEL_OVERRIDE_ENABLED = settings.get("world-level-override.enabled", false);
        SETTINGS_PASSIVE_MOBS_CHANGED_MOVEMENT_SPEED = settings.get("passive-mobs-changed-movement-speed", false);
        SETTINGS_CREATURE_NAMETAG = settings.get("creature-nametag", "&8[&7Level %level%&8 | &f%name%&8]");
        SETTINGS_CREEPER_MAX_RADIUS = settings.get("creeper-max-damage-radius", 8);

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

            if (settings.contains(path)) {
                name = settings.get(path, "INVALID_SETTINGS_FILE");
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

        int minLevel = SETTINGS_FINE_TUNING_MIN_LEVEL;

        if (SETTINGS_WORLD_LEVEL_OVERRIDE_ENABLED) {
            if (worldMinLevelMap.containsKey(worldName) && worldMinLevelMap.get(worldName) != -1) {
                return worldMinLevelMap.get(worldName);
            } else {
                if (settings.contains(worldOverridePath)) {
                    minLevel = settings.get(worldOverridePath, 10);
                    worldMinLevelMap.put(worldName, minLevel);
                    return minLevel;
                } else {
                    worldMinLevelMap.put(worldName, -1);
                }
            }
        }

        if (SETTINGS_ENTITYTYPE_LEVEL_OVERRIDE_ENABLED) {
            if (entityTypeMinLevelMap.containsKey(entityType) && entityTypeMinLevelMap.get(entityType) != -1) {
                return entityTypeMinLevelMap.get(entityType);
            } else {
                if (settings.contains(entityTypeOverridePath)) {
                    minLevel = settings.get(entityTypeOverridePath, 10);
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

        int maxLevel = SETTINGS_FINE_TUNING_MAX_LEVEL;

        if (SETTINGS_WORLD_LEVEL_OVERRIDE_ENABLED) {
            if (worldMaxLevelMap.containsKey(worldName) && worldMaxLevelMap.get(worldName) != -1) {
                return worldMaxLevelMap.get(worldName);
            } else {
                if (settings.contains(worldOverridePath)) {
                    maxLevel = settings.get(worldOverridePath, 10);
                    worldMaxLevelMap.put(worldName, maxLevel);
                    return maxLevel;
                } else {
                    worldMaxLevelMap.put(worldName, -1);
                }
            }
        }

        if (SETTINGS_ENTITYTYPE_LEVEL_OVERRIDE_ENABLED) {
            if (entityTypeMaxLevelMap.containsKey(entityType) && entityTypeMaxLevelMap.get(entityType) != -1) {
                return entityTypeMaxLevelMap.get(entityType);
            } else {
                if (settings.contains(entityTypeOverridePath)) {
                    maxLevel = settings.get(entityTypeOverridePath, 10);
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
