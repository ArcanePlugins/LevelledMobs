package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType

/**
 * Interace used for wrapping LivingEntity to provide additions common commands and information
 *
 * @author stumper66
 * @since 3.0.0
 */
interface LivingEntityInterface {
    val entityType: EntityType?

    val location: Location?

    val world: World?

    val typeName: String

    fun getApplicableRules(): MutableList<RuleInfo>

    var distanceFromSpawn: Double

    var spawnedTimeOfDay: Int

    val summonedLevel: Int?

    val wasSummoned: Boolean

    fun clearEntityData()

    fun free()
}