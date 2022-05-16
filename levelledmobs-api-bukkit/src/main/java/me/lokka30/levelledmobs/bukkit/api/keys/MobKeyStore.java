package me.lokka30.levelledmobs.bukkit.api.keys;

import com.google.common.annotations.Beta;
import org.bukkit.NamespacedKey;

/*
FIXME comment
 */
@Beta
@SuppressWarnings("unused")
public class MobKeyStore extends KeyStore {

    /*
    Represents the current level of the mob.

    Type: INTEGER
    Used: LM1, LM2, LM3, LM4
     */
    public static final NamespacedKey level = getKey("Level");

    /*
    Represents the maximum determined level of the mob.

    Type: INTEGER
    Used: LM4
     */
    public static final NamespacedKey maxLevel = getKey("MaxLevel");

    /*
    Represents the minimum determined level of the mob.

    Type: INTEGER
    Used: LM4
     */
    public static final NamespacedKey minLevel = getKey("MinLevel");

    /*
    Represents the sky light level in the location of the mob when they spawned in.
    TODO verify

    Type: INTEGER - light level
    Used: LM3, LM4
     */
    public static final NamespacedKey skyLightLevel = getKey("SkyLightLevel");

    /*
    Represents?... TODO

    Type: STRING
    Used: LM3, LM4
     */
    public static final NamespacedKey sourceSpawnerName = getKey("SourceSpawnerName");

    /*
    Represents the time of day (in ticks) in the world of the mob when they spawned in.
    TODO verify

    Type: INTEGER - world time (ticks)
    Used: LM3, LM4
     */
    public static final NamespacedKey spawnedTimeOfDay = getKey("SpawnedTimeOfDay");

    /*
    Represents if a mob was a baby mob or not when they spawned in.

    Type: STRING - "true" or "false"
    Used: LM3, LM4
     */
    public static final NamespacedKey wasBabyMob = getKey("WasBabyMob");

    /*
    Represents if a mob was summoned from `/lm summon` or not.

    Type: STRING - "true" or "false"
    Used: LM3, LM4
     */
    public static final NamespacedKey wasSummoned = getKey("WasSummoned");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey chanceRuleAllowed = getKey("ChanceRule_Allowed");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey chanceRuleDenied = getKey("ChanceRule_Denied");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey spawnerEgg = getKey("SpawnerEgg");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey spawnerEggName = getKey("SpawnerEggName");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey denyLMNametag = getKey("DenyLM_Nametag");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey hasCustomNameTag = getKey("HasCustomNameTag");

    /*
    TODO what does this key do?
     */
    @Deprecated
    public static final NamespacedKey overridenEntityName = getKey("OverridenEntityName");

    /*
    TODO what does this do?
     */
    @Deprecated
    public static final NamespacedKey playerNetherCoords = getKey("PlayerNetherCoords");

    /*
    TODO what does this do?
     */
    @Deprecated
    public static final NamespacedKey playerNetherCoordsIntoWorld = getKey("PlayerNetherCoords_IntoWorld");

    /*
    TODO what does this do?
     */
    @Deprecated
    public static final NamespacedKey overallChance = getKey("OverallChance");

}
