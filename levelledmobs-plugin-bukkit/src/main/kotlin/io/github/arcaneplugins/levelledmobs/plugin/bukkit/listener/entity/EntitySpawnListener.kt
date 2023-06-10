package io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.entity

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.LmTrigger
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntitySpawnEvent

//todo doc
class EntitySpawnListener : ListenerWrapper(imperative = true) {

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        /* handle context for this event */
        val context = Context(
            entity = event.entity,
            event = event,
            location = event.location,
            world = event.entity.world,
        )

        if (event.entity is LivingEntity)
            context.livingEntity = event.entity as LivingEntity

        /* handle triggers for this event */
        lmInstance.ruleManager.callRulesWithTrigger(
            trigger = LmTrigger.ON_ENTITY_SPAWN,
            context = context
        )
    }

}