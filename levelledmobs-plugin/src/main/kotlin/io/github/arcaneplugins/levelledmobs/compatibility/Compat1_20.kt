package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

object Compat1_20 {
    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.CAMEL,
            EntityType.SNIFFER
        )
    }

    fun all20Mobs(): MutableSet<String> {
        val names: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        names.addAll(listOf("CAMEL", "SNIFFER"))
        return names
    }
}