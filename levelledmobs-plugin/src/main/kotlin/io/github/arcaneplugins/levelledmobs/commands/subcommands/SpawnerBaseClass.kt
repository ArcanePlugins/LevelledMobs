package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

/**
 * Provides common function between SpawnerSubCommand and SpawnerEggCommand
 *
 * @author stumper66
 * @since 3.3.0
 */
abstract class SpawnerBaseClass : MessagesBase() {
    class CustomSpawnerInfo {
        @DoNotMerge
        var player: Player? = null
        var minLevel = -1
        var maxLevel = -1

        @DoNotMerge
        var noLore = false
        var delay: Int? = null
        var maxNearbyEntities: Int? = null
        var minSpawnDelay: Int? = null
        var maxSpawnDelay: Int? = null
        var requiredPlayerRange: Int? = null
        var spawnCount: Int? = null
        var spawnRange: Int? = null
        var customDropId: String? = null

        @DoNotMerge
        var customName: String? = null
        var spawnType: EntityType = EntityType.UNKNOWN

        @DoNotMerge
        var customLore: String? = null
        var lore: String? = null
    }
}