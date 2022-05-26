package me.lokka30.levelledmobs.bukkit.api.data.keys;

import com.google.common.annotations.Beta;
import org.bukkit.NamespacedKey;

/*
FIXME comment
 */
@Beta
@SuppressWarnings("unused")
public class MobKeyStore extends KeyStore {

    /*
    Represents that a mob should not receive any updates from LM's label system.

    Type: INTEGER
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey deniesLabel = getKey("DenyLM_Nametag");

    /*
    Represents the current level of the mob.

    Type: INTEGER (level)
    From: LM1, LM2, LM3, LM4

    Value      | Representation
    -----------+-----------------
    null       | mob not levelled
    non-null   | level of mob
     */
    public static final NamespacedKey level = getKey("Level");

    /*
    Represents the maximum determined level of the mob.

    Type: INTEGER (level)
    From: LM4

    Value      | Representation   | Notes
    -----------+------------------+-------------
    null       | N/A              | Not Possible
    non-null   | max level of mob | N/A
     */
    public static final NamespacedKey maxLevel = getKey("MaxLevel");

    /*
    Represents the minimum determined level of the mob.

    Type: INTEGER (level)
    From: LM4

    Value      | Representation   | Notes
    -----------+------------------+-------------
    null       | N/A              | Not Possible
    non-null   | min level of mob | N/A
     */
    public static final NamespacedKey minLevel = getKey("MinLevel");

    /*
    Represents whether the mob 'made the chance' to have any custom drops at all.

    Type: INTEGER
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey madeOverallChance = getKey("OverallChance");

    /*
    If an entity was given an overridden name in a LM function then their overridden name will be
    stored using this key in their PersistentDataContainer.

    Type: STRING (overriding entity name)
    From: LM3

    Value      | Representation
    -----------+----------------
    null       | not overridden
    non-null   | overriding name
     */
    public static final NamespacedKey overriddenName = getKey("OverridenEntityName");

    /*
    TODO make PDC keys for each player levelling variable.
    TODO add those keys to the mob data utils
     */
    public static final NamespacedKey playerLevellingId = getKey("PlayerLevellingId");

    /*
    If the mob was spawned by a LM spawner, then the name of the LM spawner will be stored using
    this key in the PDC.

    Type: STRING
    From: LM3, LM4

    Value      | Representation
    -----------+------------------------
    null       | mob not from LM spawner
    non-null   | name of LM spawner
     */
    public static final NamespacedKey sourceSpawnerName = getKey("SourceSpawnerName");

    /*
    Represents the time of day (in ticks) in the world of the mob when they spawned in.

    Type: INTEGER - world time (ticks)
    From: LM3, LM4

    Value      | Representation      | Notes
    -----------+---------------------+-------------
    null       | N/A                 | Not Possible
    non-null   | time of day (ticks) | N/A
     */
    public static final NamespacedKey spawnTimeOfDay = getKey("SpawnedTimeOfDay");

    /*
    Represents the sky light level in the location of the mob when they spawned in.

    Type: INTEGER (light level)
    From: LM3, LM4

    Value      | Representation   | Notes
    -----------+------------------+-------------
    null       | N/A              | Not Possible
    non-null   | sky light level  | N/A
     */
    public static final NamespacedKey spawnSkyLightLevel = getKey("SkyLightLevel");

    /*
    Represents if a mob was a baby mob or not when they spawned in.

    Type: INTEGER (Nullable)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey wasBabyMob = getKey("WasBabyMob");

    /*
    Represents if a mob was summoned from `/lm summon` or not.

    Type: INTEGER
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey wasSummoned = getKey("WasSummoned");

}
