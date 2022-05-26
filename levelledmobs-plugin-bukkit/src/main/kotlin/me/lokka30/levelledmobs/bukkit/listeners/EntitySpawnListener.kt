package me.lokka30.levelledmobs.bukkit.listeners

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent

/*
FIXME comment
 */
class EntitySpawnListener : ListenerWrapper(
    "org.bukkit.event.entity.EntitySpawnEvent"
) {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handle(event: EntitySpawnEvent) {
        /*
        LevelledMobs only concerns LivingEntities
         */
        if(event.entity !is LivingEntity) return

        /*
        Firstly, let's check if the entity has any non-persistent metadata to migrate.
         */
        // wasSummoned
        val wasSummonedKeyStr = EntityKeyStore.wasSummoned.toString()
        InternalEntityDataUtil.setWasSummoned(
            event.entity as LivingEntity,
            event.entity.hasMetadata(wasSummonedKeyStr) &&
                    event.entity.getMetadata(wasSummonedKeyStr).any{it.asInt() == 1}
        )
        event.entity.removeMetadata(wasSummonedKeyStr, LevelledMobs.instance!!)
    }

}