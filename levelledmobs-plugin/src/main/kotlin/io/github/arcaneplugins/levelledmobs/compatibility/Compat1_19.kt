package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

/**
 * Holds lists of entity types that are only present in minecraft 1.19 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 3.6.0
 */
object Compat1_19 {
    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.ALLAY,
            EntityType.TADPOLE,
            EntityType.FROG
        )
    }

    fun getHostileMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.WARDEN
        )
    }

    fun getAquaticMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.TADPOLE
        )
    }

    fun all19Mobs(): MutableSet<String> {
        val names: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        names.addAll(listOf("ALLAY", "TADPOLE", "FROG", "WARDEN"))
        return names
    }
}