package io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys;

import org.bukkit.NamespacedKey;

/*
FIXME comment
 */
@SuppressWarnings("unused")
public class SpawnerKeyStore extends KeyStore {

    /*
    Mobs spawned by a LM spawner will receive a custom drop ID of this key's value.

    Type: STRING (Nullable, Non-Empty)
    From: LM3, LM4

    Value    | Representation
    ---------+---------------
    null     | no custom drop ID
    non-null | custom drop ID
     */
    public static final NamespacedKey CUSTOM_DROP_ID = getKey("CustomDropID");

    /*
    Name of a LM spawner.

    Type: STRING (Non-Null, Non-Empty)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    any   | spawner name
     */
    public static final NamespacedKey CUSTOM_NAME = getKey("CustomName");

    /*
    Delay of a LM spawner.

    Type: INTEGER (Non-Null, >=1)
    From: LM3, LM4

    Value | Representation
    ------+------------------------
    any   | spawner delay (seconds)
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
     */
    public static final NamespacedKey LORE = getKey("Lore");

    /*
    Max level of a LM spawner.

    Type: INTEGER (Non-Null, >=0)
    From: LM3, LM4

    Value | Representation
    ------+------------------
    any   | spawner max level
     */
    public static final NamespacedKey MAX_LEVEL = getKey("MaxLevel");

    /*
    Max nearby entities of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------
    any   | spawner max nearby entities
     */
    public static final NamespacedKey MAX_NEARBY_ENTITIES = getKey("MaxNearbyEntities");

    /*
    Max spawn delay of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------------
    any   | spawner max spawn delay (seconds)
     */
    public static final NamespacedKey MAX_SPAWN_DELAY = getKey("MaxSpawnDelay");

    /*
    Min level of a LM spawner.

    Type: INTEGER (Non-Null, >=0)
    From: LM3, LM4

    Value | Representation
    ------+------------------
    any   | spawner min level
     */
    public static final NamespacedKey MIN_LEVEL = getKey("MinLevel");

    /*
    Min spawn delay of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+----------------------------------
    any   | spawner min spawn delay (seconds)
     */
    public static final NamespacedKey MIN_SPAWN_DELAY = getKey("MinSpawnDelay");

    /*
    Required player range of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+-------------------------------
    any   | required player range (blocks)
     */
    public static final NamespacedKey REQUIRED_PLAYER_RANGE = getKey("RequiredPlayerRange");

    /*
    Spawn count of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+------------------------
    any   | mobs to spawn per cycle
     */
    public static final NamespacedKey SPAWN_COUNT = getKey("SpawnCount");

    /*
    Spawn range of a LM spawner.

    Type: INTEGER (Non-Null, >= 1)
    From: LM3, LM4

    Value | Representation
    ------+---------------------
    any   | spawn range (blocks)
     */
    public static final NamespacedKey SPAWN_RANGE = getKey("SpawnRange");

    /*
    Entity type of a LM spawner.

    Type: STRING (Non-Null, EntityType constant)
    From: LM3, LM4

    Value | Representation
    ------+--------------------
    any   | EntityType constant
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
     */
    public static final NamespacedKey IS_SPAWNER_EGG = getKey("SpawnerEgg");

    /*
    Name of a LM spawn egg.

    Type: STRING (Non-Null, Non-Empty)
    From: LM3, LM4

    Value | Representation
    ------+---------------
    any   | spawn egg name
     */
    public static final NamespacedKey SPAWNER_EGG_NAME = getKey("SpawnerEggName");

}
