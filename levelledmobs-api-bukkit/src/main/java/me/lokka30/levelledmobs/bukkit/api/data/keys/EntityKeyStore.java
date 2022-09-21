package me.lokka30.levelledmobs.bukkit.api.data.keys;

import com.google.common.annotations.Beta;
import org.bukkit.NamespacedKey;

/*
FIXME comment
 */
@SuppressWarnings("unused")
public class EntityKeyStore extends KeyStore {

    /*
    Represents that an entity should not receive any updates from LM's label system.

    Type: Nullable Integer (bool - mob denies label)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey DENIES_LABEL = getKey("DenyLM_Nametag");

    /*
    Represents a collection of drop table IDs that an entity has.

    Type: Non-Null Set<String> (drop table IDs)
    ...type via 'MorePersistentDataTypes' library
    From: LM4

    Value    | Representation                | Notes
    ---------+-------------------------------+-----------
    null     | no drop table IDs             | Not Possible
    non-null | Set<String> of drop table IDs | Empty by default
     */
    public static final NamespacedKey DROP_TABLE_IDS = getKey("DropTableIds");

    //TODO document. stores entity uuid of father entity
    public static final NamespacedKey FATHER = getKey("Father");

    /**
     * TODO Document
     */
    public static final NamespacedKey INHERITANCE_BREEDING_FORMULA =
        getKey("InheritanceBreedingFormula");

    /**
     * TODO Document
     */
    public static final NamespacedKey INHERITANCE_TRANSFORMATION_FORMULA =
        getKey("InheritanceTransformationFormula");

    /*
    Represents the current level of the entity.

    Type: Nullable Integer (current level)
    From: LM1, LM2, LM3, LM4

    Value      | Representation
    -----------+-----------------
    null       | entity not levelled
    non-null   | level of entity
     */
    public static final NamespacedKey LEVEL = getKey("Level");

    /*
    Represents whether the entity 'made the chance' to have any custom drops at all.

    Type: Nullable Integer (bool - made overall chance)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey MADE_OVERALL_CHANCE = getKey("OverallChance");

    /*
    Represents the maximum determined level of the entity.

    Type: Non-Null Integer (level)
    From: LM4

    Value      | Representation   | Notes
    -----------+------------------+-------------
    null       | N/A              | Not Possible
    non-null   | max level of ent | N/A
     */
    public static final NamespacedKey MAX_LEVEL = getKey("MaxLevel");

    /*
    Represents the minimum determined level of the entity.

    Type: Non-Null Integer (level)
    From: LM4

    Value      | Representation      | Notes
    -----------+---------------------+-------------
    null       | N/A                 | Not Possible
    non-null   | min level of entity | N/A
     */
    public static final NamespacedKey MIN_LEVEL = getKey("MinLevel");

    //TODO document. stores entity uuid of mother entity
    public static final NamespacedKey MOTHER = getKey("Mother");

    /*
    If an entity was given an overridden name in a LM function then their overridden name will be
    stored using this key in their PersistentDataContainer.

    Type: Nullable String (overriding entity name)
    From: LM3

    Value      | Representation
    -----------+----------------
    null       | not overridden
    non-null   | overriding name
     */
    public static final NamespacedKey OVERRIDEN_ENTITY_NAME = getKey("OverridenEntityName");

    /*
    TODO make PDC keys for each player levelling variable.
    TODO add those keys to the entity data utils
     */
    @Beta
    public static final NamespacedKey PLAYER_LEVELLING_ID = getKey("PlayerLevellingId");

    /*
    If the entity was spawned by a LM spawner, then the name of the LM spawner will be stored using
    this key in the PDC.

    Type: Nullable String (source spawner name)
    From: LM3, LM4

    Value      | Representation
    -----------+------------------------
    null       | entity not from LM spawner
    non-null   | name of LM spawner
     */
    public static final NamespacedKey SOURCE_SPAWNER_NAME = getKey("SourceSpawnerName");

    /*
    Represents the time of day (in ticks) in the world of the entity when they spawned in.

    Type: Non-Null Integer (world time in ticks)
    From: LM3, LM4

    Value      | Representation      | Notes
    -----------+---------------------+-------------
    null       | N/A                 | Not Possible
    non-null   | time of day (ticks) | N/A
     */
    public static final NamespacedKey SPAWNED_TIME_OF_DAY = getKey("SpawnedTimeOfDay");

    /*
    Represents the sky light level in the location of the entity when they spawned in.

    Type: Non-Null Integer (light level)
    From: LM3, LM4

    Value      | Representation   | Notes
    -----------+------------------+-------------
    null       | N/A              | Not Possible
    non-null   | sky light level  | N/A
     */
    public static final NamespacedKey SPAWNED_SKY_LIGHT_LEVEL = getKey("SkyLightLevel");

    /*
    Represents if an entity was a baby or not when they spawned in.

    Type: Nullable Integer (bool - was baby)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey WAS_BABY = getKey("WasBabyMob");

    /**
     * TODO Document
     */
    public static final NamespacedKey WAS_BRED = getKey("WasBred");

    /*
    Represents if an entity was summoned from `/lm summon` or not.

    Type: Nullable Integer (bool - was summoned)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A
     */
    public static final NamespacedKey WAS_SUMMONED = getKey("WasSummoned");

    /**
     * TODO Document
     */
    public static final NamespacedKey WAS_TRANSFORMED = getKey("WasTransformed");

}
