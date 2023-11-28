package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.ChanceCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityBiomeCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityCustomNameContains
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityLevelCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityOwnerCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityTypeCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityWorldCondition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.PlayerWorldCondition
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class ConditionParseListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onConditionParse(event: ConditionParseEvent){
        val process = event.process
        val node = event.node

        when (event.identifier.lowercase()) {
            "chance" -> addCondition(event, ChanceCondition(process, node))
            "entity-biome" -> addCondition(event, EntityBiomeCondition(process, node))
            "entity-custom-name-contains" -> addCondition(event, EntityCustomNameContains(process, node))
            "entity-level" -> addCondition(event, EntityLevelCondition(process, node))
            "entity-owner" -> addCondition(event, EntityOwnerCondition(process, node))
            "entity-type" -> addCondition(event, EntityTypeCondition(process, node))
            "entity-world" -> addCondition(event, EntityWorldCondition(process, node))
            "player-world" -> addCondition(event, PlayerWorldCondition(process, node))
        }
    }

    private fun addCondition(event: ConditionParseEvent, condition: Condition) {
        event.process.conditions.add(condition)
        event.claimed = true
    }
}