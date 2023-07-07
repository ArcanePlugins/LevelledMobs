/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.api.bukkit.keys;

import org.bukkit.NamespacedKey;

/**
 * TODO Document
 * <p>
 * Some of the key IDs appear to use an inconsistent naming formula - this is because they were
 * grandfathered in to provide greater compatibility with legacy levelled mobs from LM3.
 *
 * @author lokka30
 */
@SuppressWarnings("unused")
public class EntityKeyStore extends KeyStore {

    //TODO document
    public static final NamespacedKey CREEPER_BLAST_DAMAGE_MULTIPLIER_FORMULA = getKey(
        "CreeperBlastDamageMultiplierFormula");

    //TODO Document
    public static final NamespacedKey DEATH_LABEL_FORMULA = getKey("DeathLabelFormula");

    /*
    Represents that an entity should not receive any updates from LM's label system.

    Type: Nullable Integer (bool - mob denies label)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A

    TODO reformat as javadoc
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

    TODO reformat as javadoc
     */
    public static final NamespacedKey DROP_TABLE_IDS = getKey("DropTableIds");

    //TODO document
    public static final NamespacedKey EXP_DROP_MULTIPLIER_FORMULA = getKey(
        "ExpDropMultiplierFormula");

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

    //TODO document
    public static final NamespacedKey ITEM_DROP_MULTIPLIER_FORMULA = getKey(
        "ItemDropMultiplierFormula");

    /**
     * TODO DOC
     */
    public static final NamespacedKey LABEL_HANDLER_FORMULAS = getKey("LabelHandlers");

    /*
    Represents the current level of the entity.

    Type: Nullable Integer (current level)
    From: LM1, LM2, LM3, LM4

    Value      | Representation
    -----------+-----------------
    null       | entity not levelled
    non-null   | level of entity

    TODO reformat as javadoc
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

    TODO reformat as javadoc
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

    TODO reformat as javadoc
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

    TODO reformat as javadoc
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

    TODO reformat as javadoc
     */
    public static final NamespacedKey OVERRIDEN_ENTITY_NAME = getKey("OverridenEntityName");

    //TODO document – see https://github.com/lokka30/LevelledMobs/issues/415
    public static final NamespacedKey PRIMARY_LABEL_HANDLER = getKey("PrimaryLabelHandler");

    //TODO document
    public static final NamespacedKey SHIELD_BREAKER_MULTIPLIER_FORMULA = getKey(
        "ShieldBreakerMultiplierFormula");

    /*
    If the entity was spawned by a LM spawner, then the name of the LM spawner will be stored using
    this key in the PDC.

    Type: Nullable String (source spawner name)
    From: LM3, LM4

    Value      | Representation
    -----------+------------------------
    null       | entity not from LM spawner
    non-null   | name of LM spawner

    TODO reformat as javadoc
     */
    public static final NamespacedKey SOURCE_SPAWNER_NAME = getKey("SourceSpawnerName");

    // TODO Document (since LM3)
    public static final NamespacedKey SPAWN_REASON = getKey("SpawnReason");

    /*
    Represents the time of day (in ticks) in the world of the entity when they spawned in.

    Type: Non-Null Integer (world time in ticks)
    From: LM3, LM4

    Value      | Representation      | Notes
    -----------+---------------------+-------------
    null       | N/A                 | Not Possible
    non-null   | time of day (ticks) | N/A

    TODO reformat as javadoc
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

    TODO reformat as javadoc
     */
    public static final NamespacedKey SPAWNED_SKY_LIGHT_LEVEL = getKey("SkyLightLevel");

    /*
    TODO use this key
    TODO add methods to the entity data utils
     */
    public static final NamespacedKey VARIABLE_LEVELLING_FORMULA = getKey(
        "VariableLevellingFormula");

    /*
    Represents if an entity was a baby or not when they spawned in.

    Type: Nullable Integer (bool - was baby)
    From: LM3, LM4

    Value | Representation | Notes
    ------+----------------+-----------
    null  | false          | Deprecated
    0     | false          | N/A
    1     | true           | N/A

    TODO reformat as javadoc
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

    TODO reformat as javadoc
     */
    public static final NamespacedKey WAS_SUMMONED = getKey("WasSummoned");

    /**
     * TODO Document
     */
    public static final NamespacedKey WAS_TRANSFORMED = getKey("WasTransformed");

    //TODO document
    public static final NamespacedKey[] LEVEL_RELATED_KEYS = new NamespacedKey[]{
        DROP_TABLE_IDS,
        INHERITANCE_BREEDING_FORMULA,
        INHERITANCE_TRANSFORMATION_FORMULA,
        LEVEL,
        MADE_OVERALL_CHANCE,
        MAX_LEVEL,
        MIN_LEVEL,
        OVERRIDEN_ENTITY_NAME,
        VARIABLE_LEVELLING_FORMULA
    };

}

