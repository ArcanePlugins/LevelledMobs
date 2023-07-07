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

/*
TODO doc
 */
@SuppressWarnings("unused")
public class SpawnerKeyStore extends KeyStore {

    /**
     * Mobs spawned by a LM spawner will receive a custom drop ID of this key's value.
     * <pre>
     * {@code
     * Type: STRING (Nullable, Non-Empty)
     * From: LM3, LM4
     * Value    | Representation
     * ---------+---------------
     * null     | no custom drop ID
     * non-null | custom drop ID
     * }
     * </pre>
     */
    public static final NamespacedKey CUSTOM_DROP_ID = getKey("CustomDropID");

    /**
     * Name of a LM spawner.
     * <pre>
     * {@code
     * Type: STRING (Non-Null, Non-Empty)
     * From: LM3, LM4
     * Value | Representation
     * ------+---------------
     * any   | spawner name
     * }
     * </pre>
     */
    public static final NamespacedKey CUSTOM_NAME = getKey("CustomName");

    /*
    Delay of a LM spawner.

    Type: INTEGER (Non-Null, >=1)
    From: LM3, LM4

    Value | Representation
    ------+------------------------
    any   | spawner delay (seconds)

    TODO reformat as javadoc
     */
    public static final NamespacedKey DELAY = getKey("Delay");

    /*
    Lore of a LM spawner.
    Line breaks are achieved using the newline chracter (`\n`).

    Type: STRING (Non-Null, Non-Empty)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    any   | spawner lore

    TODO reformat as javadoc
     */
    public static final NamespacedKey LORE = getKey("Lore");

    /*
    Max level of a LM spawner.

    Type: INTEGER (Non-Null, >=0)
    From: LM3, LM4

    Value | Representation
    ------+------------------
    any   | spawner max level

    TODO reformat as javadoc
     */
    public static final NamespacedKey MAX_LEVEL = getKey("MaxLevel");

    /*
    Max nearby entities of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------
    any   | spawner max nearby entities

    TODO reformat as javadoc
     */
    public static final NamespacedKey MAX_NEARBY_ENTITIES = getKey("MaxNearbyEntities");

    /*
    Max spawn delay of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------------
    any   | spawner max spawn delay (seconds)

    TODO reformat as javadoc
     */
    public static final NamespacedKey MAX_SPAWN_DELAY = getKey("MaxSpawnDelay");

    /*
    Min level of a LM spawner.

    Type: INTEGER (Non-Null, >=0)
    From: LM3, LM4

    Value | Representation
    ------+------------------
    any   | spawner min level

    TODO reformat as javadoc
     */
    public static final NamespacedKey MIN_LEVEL = getKey("MinLevel");

    /*
    Min spawn delay of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------------
    any   | spawner min spawn delay (seconds)

    TODO reformat as javadoc
     */
    public static final NamespacedKey MIN_SPAWN_DELAY = getKey("MinSpawnDelay");

    /*
    Required player range of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+-------------------------------
    any   | required player range (blocks)

    TODO reformat as javadoc
     */
    public static final NamespacedKey REQUIRED_PLAYER_RANGE = getKey("RequiredPlayerRange");

    /*
    Spawn count of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+------------------------
    any   | mobs to spawn per cycle

    TODO reformat as javadoc
     */
    public static final NamespacedKey SPAWN_COUNT = getKey("SpawnCount");

    /*
    Spawn range of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+---------------------
    any   | spawn range (blocks)

    TODO reformat as javadoc
     */
    public static final NamespacedKey SPAWN_RANGE = getKey("SpawnRange");

    /*
    Entity type of a LM spawner.

    Type: STRING (Non-Null, EntityType constant)
    From: LM3, LM4

    Value | Representation
    ------+--------------------
    any   | EntityType constant

    TODO reformat as javadoc
     */
    public static final NamespacedKey SPAWN_TYPE = getKey("SpawnType");

    /*
    Indicates if a spawner is a LM spawner.

    Type: INTEGER (Nullable)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    null  | false
    0     | false
    1     | true

    TODO reformat as javadoc
     */
    public static final NamespacedKey IS_SPAWNER = getKey("Spawner");

    /*
    Indicates if a spawn egg is a LM spawn egg.

    Type: INTEGER (Nullable)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    null  | false
    0     | false
    1     | true

    TODO reformat as javadoc
     */
    public static final NamespacedKey IS_SPAWNER_EGG = getKey("SpawnerEgg");

    /*
    Name of a LM spawn egg.

    Type: STRING (Non-Null, Non-Empty)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    any   | spawn egg name

    TODO reformat as javadoc
     */
    public static final NamespacedKey SPAWNER_EGG_NAME = getKey("SpawnerEggName");

}
