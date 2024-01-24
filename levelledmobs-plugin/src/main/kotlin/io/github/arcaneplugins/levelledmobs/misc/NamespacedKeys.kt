package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.NamespacedKey

/**
 * Holds all of the NamespacedKeys used by LevelledMobs
 *
 * @author stumper66
 * @since 3.2.0
 */
object NamespacedKeys {
    val levelKey = NamespacedKey(LevelledMobs.instance, "level")
    val spawnReasonKey = NamespacedKey(LevelledMobs.instance, "spawnReason")
    val noLevelKey = NamespacedKey(LevelledMobs.instance, "noLevel")
    val wasBabyMobKey = NamespacedKey(LevelledMobs.instance, "wasBabyMob")
    val overridenEntityNameKey = NamespacedKey(LevelledMobs.instance, "overridenEntityName")
    val hasCustomNameTag = NamespacedKey(LevelledMobs.instance, "hasCustomNameTag")
    val playerLevellingId = NamespacedKey(LevelledMobs.instance, "playerLevelling_Id")
    val playerLevellingValue = NamespacedKey(LevelledMobs.instance, "playerLevelling_Value")
    val chanceRuleAllowed = NamespacedKey(LevelledMobs.instance, "chanceRule_Allowed")
    val chanceRuleDenied = NamespacedKey(LevelledMobs.instance, "chanceRule_Denied")
    val denyLmNametag = NamespacedKey(LevelledMobs.instance, "denyLM_Nametag")
    val sourceSpawnerName = NamespacedKey(LevelledMobs.instance, "sourceSpawnerName")
    val spawnedTimeOfDay = NamespacedKey(LevelledMobs.instance, "spawnedTimeOfDay")
    val wasSummoned = NamespacedKey(LevelledMobs.instance, "wasSummoned")
    val playerNetherCoords = NamespacedKey(LevelledMobs.instance, "playerNetherCoords")
    val playerNetherCoordsIntoWorld = NamespacedKey(LevelledMobs.instance, "playerNetherCoords_IntoWorld")
    val skyLightLevel: NamespacedKey = NamespacedKey(LevelledMobs.instance, "skyLightLevel")
    val playerLevellingSourceNumber = NamespacedKey(LevelledMobs.instance, "playerLevellingSourceNumber")
    val lastDamageTime = NamespacedKey(LevelledMobs.instance, "lastDamageTime")
    val mobHash: NamespacedKey = NamespacedKey(LevelledMobs.instance, "mobHash")
    val pickedUpItems: NamespacedKey = NamespacedKey(LevelledMobs.instance, "pickedUpItems")
    val lockSettings = NamespacedKey(LevelledMobs.instance, "lockSettings")
    val lockedNametag = NamespacedKey(LevelledMobs.instance, "lockedNametag")
    val lockedNameOverride = NamespacedKey(LevelledMobs.instance, "lockedNameOverride")
    val lockedDropRules = NamespacedKey(LevelledMobs.instance, "lockedDropRules")
    val lockedDropRulesOverride = NamespacedKey(LevelledMobs.instance, "lockedDropRulesOverride")
    val spawnerEgg = NamespacedKey(LevelledMobs.instance, "spawnerEgg")
    val spawnerEggName = NamespacedKey(LevelledMobs.instance, "spawnerEggName")
    val keySpawner = NamespacedKey(LevelledMobs.instance, "spawner")
    val keySpawnerMinLevel = NamespacedKey(LevelledMobs.instance, "minlevel")
    val keySpawnerMaxLevel = NamespacedKey(LevelledMobs.instance, "maxlevel")
    val keySpawnerCustomDropId = NamespacedKey(LevelledMobs.instance, "customdropid")
    val keySpawnerDelay = NamespacedKey(LevelledMobs.instance, "delay")
    val keySpawnerMaxNearbyEntities = NamespacedKey(LevelledMobs.instance, "maxnearbyentities")
    val keySpawnerMinSpawnDelay = NamespacedKey(LevelledMobs.instance, "minspawndelay")
    val keySpawnerMaxSpawnDelay = NamespacedKey(LevelledMobs.instance, "maxspawndelay")
    val keySpawnerRequiredPlayerRange = NamespacedKey(LevelledMobs.instance, "requiredplayerrange")
    val keySpawnerSpawnCount = NamespacedKey(LevelledMobs.instance, "spawncount")
    val keySpawnerSpawnType = NamespacedKey(LevelledMobs.instance, "spawntype")
    val keySpawnerSpawnRange = NamespacedKey(LevelledMobs.instance, "spawnrange")
    val keySpawnerCustomName = NamespacedKey(LevelledMobs.instance, "customname")
    val keySpawnerLore = NamespacedKey(LevelledMobs.instance, "lore")
    val overallChanceKey = NamespacedKey(LevelledMobs.instance, "pickedUpItems")
}