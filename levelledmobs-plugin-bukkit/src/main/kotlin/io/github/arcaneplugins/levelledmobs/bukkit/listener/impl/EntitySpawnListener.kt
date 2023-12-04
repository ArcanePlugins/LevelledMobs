package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.EntityKeyStore
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setSpawnReason
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setWasSummoned
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler.getDefinedCustomDropsForEntity
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.StandardCustomDropType
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.metadata.MetadataValue

class EntitySpawnListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handle(event: EntitySpawnEvent)
    {
        debug(DebugCategory.LISTENERS) {"EntitySpawnListener is handling an event."}

        /*
        LevelledMobs only concerns LivingEntities
         */

        if (event.entity !is LivingEntity) return
        val entity = event.entity as LivingEntity

        /*
        Check if the entity has any non-persistent metadata to migrate
         */
        // wasSummoned
        val wasSummonedKeyStr = EntityKeyStore.WAS_SUMMONED.toString()
        setWasSummoned(entity,entity.hasMetadata(wasSummonedKeyStr) &&
                    entity.getMetadata(wasSummonedKeyStr).stream()
                        .anyMatch { `val`: MetadataValue -> `val`.asInt() == 1 },
            true
        )
        entity.removeMetadata(wasSummonedKeyStr, LevelledMobs.lmInstance)

        /*
        Add other data
         */

        if (event is CreatureSpawnEvent) {
            // Set spawn reason of entity
            setSpawnReason(
                event.entity,
                event.spawnReason,
                true
            )
        }

        /*
        Fire the associated trigger.
         */

        runFunctionsWithTriggers(
            Context(entity),
            mutableListOf("on-entity-spawn")
        )

        /*
        Custom Drops
         */

        handleCustomDrops(event)
    }

    private fun handleCustomDrops(
        event: EntitySpawnEvent
    ){
        // This is a safe cast since LM will only call this after it has verified this is a LivngEnt
        val entity = event.entity as LivingEntity

        val context = Context(entity).withEvent(event)

        val cds: Collection<CustomDrop> = getDefinedCustomDropsForEntity(
            entity,
            context
        )

        for (cd in cds) {
            if (cd.type == StandardCustomDropType.ITEM.name) {
                val icd = cd as ItemCustomDrop
                icd.attemptToApplyEquipment(entity)
            } else if (cd.type.equals(StandardCustomDropType.COMMAND.name, ignoreCase = true)) {
                val ccd = cd as CommandCustomDrop
                if (ccd.commandRunEvents.contains(CustomDropsEventType.ON_SPAWN.name)) {
                    ccd.execute(CustomDropsEventType.ON_SPAWN, context)
                }
            }
        }
    }
}