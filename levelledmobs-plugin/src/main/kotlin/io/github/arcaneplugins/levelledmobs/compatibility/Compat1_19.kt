package io.github.arcaneplugins.levelledmobs.compatibility

import java.util.TreeSet
import org.bukkit.entity.EntityType

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