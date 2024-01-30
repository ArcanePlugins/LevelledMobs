package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

/**
 * Holds lists of entity types that are only present in minecraft 1.17 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 3.0.0
 */
object Compat117 {
    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.AXOLOTL,
            EntityType.GLOW_SQUID,
            EntityType.GOAT
        )
    }

    fun getForceBlockedEntityType(): MutableCollection<EntityType> {
        return mutableSetOf(EntityType.GLOW_ITEM_FRAME, EntityType.MARKER)
    }

    fun all17Mobs(): MutableSet<String> {
        val names: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        names.addAll(listOf("AXOLOTL", "GLOW_SQUID", "GOAT"))
        return names
    }
}