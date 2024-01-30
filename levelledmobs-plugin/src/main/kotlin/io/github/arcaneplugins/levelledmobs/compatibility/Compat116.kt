package io.github.arcaneplugins.levelledmobs.compatibility

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.entity.EntityType

/**
 * Holds lists of entity types that are only present in minecraft 1.16 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 2.4.0
 */
object Compat116 {
    fun getHostileMobs(): MutableSet<EntityType> {
        return if (shouldIncludePiglinBrutes()) {
            mutableSetOf(
                EntityType.HOGLIN,
                EntityType.PIGLIN,
                EntityType.PIGLIN_BRUTE
            )
        } else {
            mutableSetOf(
                EntityType.HOGLIN,
                EntityType.PIGLIN
            )
        }
    }

    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.STRIDER,
            EntityType.ZOMBIFIED_PIGLIN
        )
    }

    private fun shouldIncludePiglinBrutes(): Boolean {
        val ver = LevelledMobs.instance.ver
        if (ver.minecraftVersion >= 1.17) return true
        return ver.revision >= 2

        // 1.16.2+
    }
}