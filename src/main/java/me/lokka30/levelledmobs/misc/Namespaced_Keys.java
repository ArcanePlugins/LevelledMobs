package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;

/**
 * Holds all of the NamespacedKeys used by LevelledMobs
 *
 * @author stumper66
 * @since 3.2.0
 */
public class Namespaced_Keys {
    public Namespaced_Keys(final LevelledMobs main){
        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        noLevelKey = new NamespacedKey(main, "noLevel");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        hasCustomNameTag = new NamespacedKey(main, "hasCustomNameTag");
        playerLevelling_Id = new NamespacedKey(main, "playerLevelling_Id");
        chanceRule_Allowed = new NamespacedKey(main, "chanceRule_Allowed");
        chanceRule_Denied = new NamespacedKey(main, "chanceRule_Denied");
        denyLM_Nametag = new NamespacedKey(main, "denyLM_Nametag");
        sourceSpawnerName = new NamespacedKey(main, "sourceSpawnerName");
        spawnedTimeOfDay = new NamespacedKey(main, "spawnedTimeOfDay");
        wasSummoned = new NamespacedKey(main, "wasSummoned");

        spawnerEgg = new NamespacedKey(main, "spawnerEgg");
        keySpawner = new NamespacedKey(main, "spawner");
        keySpawner_MinLevel = new NamespacedKey(main, "minlevel");
        keySpawner_MaxLevel = new NamespacedKey(main, "maxlevel");
        keySpawner_CustomDropId = new NamespacedKey(main, "customdropid");
        keySpawner_Delay = new NamespacedKey(main, "delay");
        keySpawner_MaxNearbyEntities = new NamespacedKey(main, "maxnearbyentities");
        keySpawner_MinSpawnDelay = new NamespacedKey(main, "minspawndelay");
        keySpawner_MaxSpawnDelay = new NamespacedKey(main, "maxspawndelay");
        keySpawner_RequiredPlayerRange = new NamespacedKey(main, "requiredplayerrange");
        keySpawner_SpawnCount = new NamespacedKey(main, "spawncount");
        keySpawner_SpawnType = new NamespacedKey(main, "spawntype");
        keySpawner_SpawnRange = new NamespacedKey(main, "spawnrange");
        keySpawner_CustomName = new NamespacedKey(main, "customname");
        keySpawner_Lore = new NamespacedKey(main, "lore");

        this.overallChanceKey = new NamespacedKey(main, "overallChance");
    }

    public final NamespacedKey levelKey; // This stores the mob's level.
    public final NamespacedKey spawnReasonKey; //This is stored on levelled mobs to tell how a mob was spawned
    public final NamespacedKey noLevelKey; // This key tells LM not to level the mob in future
    public final NamespacedKey wasBabyMobKey; // This key tells LM not to level the mob in future
    public final NamespacedKey overridenEntityNameKey;
    public final NamespacedKey hasCustomNameTag;
    public final NamespacedKey playerLevelling_Id;
    public final NamespacedKey chanceRule_Allowed;
    public final NamespacedKey chanceRule_Denied;
    public final NamespacedKey denyLM_Nametag;
    public final NamespacedKey sourceSpawnerName;
    public final NamespacedKey spawnedTimeOfDay;
    public final NamespacedKey wasSummoned;

    final public NamespacedKey spawnerEgg;
    final public NamespacedKey keySpawner;
    final public NamespacedKey keySpawner_MinLevel;
    final public NamespacedKey keySpawner_MaxLevel;
    final public NamespacedKey keySpawner_CustomDropId;
    final public NamespacedKey keySpawner_Delay;
    final public NamespacedKey keySpawner_MaxNearbyEntities;
    final public NamespacedKey keySpawner_MinSpawnDelay;
    final public NamespacedKey keySpawner_MaxSpawnDelay;
    final public NamespacedKey keySpawner_RequiredPlayerRange;
    final public NamespacedKey keySpawner_SpawnCount;
    final public NamespacedKey keySpawner_SpawnType;
    final public NamespacedKey keySpawner_SpawnRange;
    final public NamespacedKey keySpawner_CustomName;
    final public NamespacedKey keySpawner_Lore;

    public final NamespacedKey overallChanceKey;
}
