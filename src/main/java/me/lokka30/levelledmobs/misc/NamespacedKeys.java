package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;

/**
 * Holds all of the NamespacedKeys used by LevelledMobs
 *
 * @author stumper66
 * @since 3.2.0
 */
public class NamespacedKeys {

    public NamespacedKeys(final LevelledMobs main) {
        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        noLevelKey = new NamespacedKey(main, "noLevel");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        hasCustomNameTag = new NamespacedKey(main, "hasCustomNameTag");
        playerLevellingId = new NamespacedKey(main, "playerLevelling_Id");
        chanceRuleAllowed = new NamespacedKey(main, "chanceRule_Allowed");
        chanceRuleDenied = new NamespacedKey(main, "chanceRule_Denied");
        denyLmNametag = new NamespacedKey(main, "denyLM_Nametag");
        sourceSpawnerName = new NamespacedKey(main, "sourceSpawnerName");
        spawnedTimeOfDay = new NamespacedKey(main, "spawnedTimeOfDay");
        wasSummoned = new NamespacedKey(main, "wasSummoned");
        playerNetherCoords = new NamespacedKey(main, "playerNetherCoords");
        playerNetherCoordsIntoWorld = new NamespacedKey(main, "playerNetherCoords_IntoWorld");
        skyLightLevel = new NamespacedKey(main, "skyLightLevel");
        lockSettings = new NamespacedKey(main, "lockSettings");
        lockedNametag = new NamespacedKey(main, "lockedNametag");
        lockedNameOverride = new NamespacedKey(main, "lockedNameOverride");
        lockedDropRules = new NamespacedKey(main, "lockedDropRules");
        lockedDropRulesOverride = new NamespacedKey(main, "lockedDropRulesOverride");
        playerLevellingSourceNumber = new NamespacedKey(main, "playerLevellingSourceNumber");
        lastLevelledTime = new NamespacedKey(main, "lastLevelledTime");

        spawnerEgg = new NamespacedKey(main, "spawnerEgg");
        spawnerEggName = new NamespacedKey(main, "spawnerEggName");
        keySpawner = new NamespacedKey(main, "spawner");
        keySpawnerMinLevel = new NamespacedKey(main, "minlevel");
        keySpawnerMaxLevel = new NamespacedKey(main, "maxlevel");
        keySpawnerCustomDropId = new NamespacedKey(main, "customdropid");
        keySpawnerDelay = new NamespacedKey(main, "delay");
        keySpawnerMaxNearbyEntities = new NamespacedKey(main, "maxnearbyentities");
        keySpawnerMinSpawnDelay = new NamespacedKey(main, "minspawndelay");
        keySpawnerMaxSpawnDelay = new NamespacedKey(main, "maxspawndelay");
        keySpawnerRequiredPlayerRange = new NamespacedKey(main, "requiredplayerrange");
        keySpawnerSpawnCount = new NamespacedKey(main, "spawncount");
        keySpawnerSpawnType = new NamespacedKey(main, "spawntype");
        keySpawnerSpawnRange = new NamespacedKey(main, "spawnrange");
        keySpawnerCustomName = new NamespacedKey(main, "customname");
        keySpawnerLore = new NamespacedKey(main, "lore");

        this.overallChanceKey = new NamespacedKey(main, "overallChance");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey spawnReasonKey; //This is stored on levelled mobs to tell how a mob was spawned
    public final NamespacedKey noLevelKey; // This key tells LM not to level the mob in future
    public final NamespacedKey wasBabyMobKey; // This key tells LM not to level the mob in future
    public final NamespacedKey overridenEntityNameKey;
    public final NamespacedKey hasCustomNameTag;
    public final NamespacedKey playerLevellingId;
    public final NamespacedKey chanceRuleAllowed;
    public final NamespacedKey chanceRuleDenied;
    public final NamespacedKey denyLmNametag;
    public final NamespacedKey sourceSpawnerName;
    public final NamespacedKey spawnedTimeOfDay;
    public final NamespacedKey wasSummoned;
    public final NamespacedKey playerNetherCoords;
    public final NamespacedKey playerNetherCoordsIntoWorld;
    public final NamespacedKey skyLightLevel;
    public final NamespacedKey playerLevellingSourceNumber;
    public final NamespacedKey lastLevelledTime;

    public final NamespacedKey lockSettings;
    public final NamespacedKey lockedNametag;
    public final NamespacedKey lockedNameOverride;
    public final NamespacedKey lockedDropRules;
    public final NamespacedKey lockedDropRulesOverride;

    final public NamespacedKey spawnerEgg;
    final public NamespacedKey spawnerEggName;
    final public NamespacedKey keySpawner;
    final public NamespacedKey keySpawnerMinLevel;
    final public NamespacedKey keySpawnerMaxLevel;
    final public NamespacedKey keySpawnerCustomDropId;
    final public NamespacedKey keySpawnerDelay;
    final public NamespacedKey keySpawnerMaxNearbyEntities;
    final public NamespacedKey keySpawnerMinSpawnDelay;
    final public NamespacedKey keySpawnerMaxSpawnDelay;
    final public NamespacedKey keySpawnerRequiredPlayerRange;
    final public NamespacedKey keySpawnerSpawnCount;
    final public NamespacedKey keySpawnerSpawnType;
    final public NamespacedKey keySpawnerSpawnRange;
    final public NamespacedKey keySpawnerCustomName;
    final public NamespacedKey keySpawnerLore;

    public final NamespacedKey overallChanceKey;
}
