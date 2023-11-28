package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setFather
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setMother
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setWasTransformed
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTransformEvent

class EntityTransformListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handle(event: EntityTransformEvent){
        val lparent = event.entity as? LivingEntity ?: return

        event.transformedEntities.stream()
            .filter { child: Entity? -> child is LivingEntity }
            .map { child: Entity? -> child as LivingEntity? }
            .forEach { child: LivingEntity? ->
                setFather(child!!, lparent, true)
                setMother(child, lparent, true)
                setWasTransformed(child, true, true)

                /*
                Fire the associated trigger.
                 */
                runFunctionsWithTriggers(
                Context(child)
                    .withFather(lparent)
                    .withMother(lparent), mutableListOf("on-entity-transform")
                )
            }
    }
}