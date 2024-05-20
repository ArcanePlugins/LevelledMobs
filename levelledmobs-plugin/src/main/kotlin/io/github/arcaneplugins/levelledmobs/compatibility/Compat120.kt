package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

/**
 * Holds lists of entity types that are only present in minecraft 1.20 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 3.14.0
 */
object Compat120 {
    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.CAMEL,
            EntityType.SNIFFER
        )
    }

    fun all20Mobs(): MutableSet<String> {
        val names: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
        names.addAll(listOf("CAMEL", "SNIFFER"))
        return names
    }
}