package me.lokka30.levelledmobs.bukkit.listeners

import me.lokka30.levelledmobs.bukkit.data.InternalMobDataUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor.GRAY
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.metadata.MetadataValue

/*
FIXME comment
 */
class EntitySpawnListener : ListenerWrapper(
    "org.bukkit.event.entity.EntitySpawnEvent"
) {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handle(event: EntitySpawnEvent) {
        //FIXME more
        if(event.entity !is LivingEntity) return
        Bukkit.broadcastMessage("${GRAY}A mob spawned in.")
        handleWasSummoned(event.entity as LivingEntity)
    }

    private fun handleWasSummoned(mob: LivingEntity) {
        val key = "LevelledMobs:WasSummoned".lowercase()
        if(mob.hasMetadata(key)) {
            val predicate: (MetadataValue) -> Boolean = {it.asInt() == 1}
            InternalMobDataUtil.setWasSummoned(mob, mob.getMetadata(key).any(predicate))
        }
        //TODO
    }

}