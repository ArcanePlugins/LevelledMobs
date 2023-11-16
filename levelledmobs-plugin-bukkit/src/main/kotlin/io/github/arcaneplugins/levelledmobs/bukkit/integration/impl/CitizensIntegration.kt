package io.github.arcaneplugins.levelledmobs.bukkit.integration.impl

import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.entity.EntityOwner

import io.github.arcaneplugins.levelledmobs.bukkit.api.util.TriState
import io.github.arcaneplugins.levelledmobs.bukkit.integration.Integration
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationPriority
import org.bukkit.entity.LivingEntity


class CitizensIntegration : EntityOwner, Integration(
    "Citizens",
    "Detects entities which are Citizens NPCs",
    true,
    true,
    IntegrationPriority.NORMAL){

    fun ownsEntity(entity: LivingEntity): TriState{
        // TODO test this.
        return TriState.of(entity.hasMetadata("NPC"))
    }
}