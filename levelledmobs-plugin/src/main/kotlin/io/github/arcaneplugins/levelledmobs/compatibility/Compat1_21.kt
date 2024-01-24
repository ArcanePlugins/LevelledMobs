package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

object Compat1_21 {
    fun getPassiveMobs(): MutableSet<EntityType> {
        return mutableSetOf(
            EntityType.valueOf("ARMADILLO")
        )
    }

    fun all21Mobs(): MutableSet<String> {
        val names: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        names.add("ARMADILLO")
        return names
    }
}