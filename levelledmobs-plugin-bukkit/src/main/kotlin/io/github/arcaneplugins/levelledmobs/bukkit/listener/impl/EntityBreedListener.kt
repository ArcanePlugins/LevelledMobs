package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setFather
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setMother
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setWasBred
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityBreedEvent

class EntityBreedListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun handle(event: EntityBreedEvent){
        val child = event.entity
        val father = event.father
        val mother = event.mother

        setFather(child, father, true)
        setMother(child, mother, true)
        setWasBred(child, true, true)

        runFunctionsWithTriggers(
            Context()
                .withEntity(child)
                .withFather(father)
                .withMother(mother)
            , mutableListOf("on-entity-breed")
        )
    }
}