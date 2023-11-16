package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import org.bukkit.entity.EntityType

class EntityTypeRecipient(
    drops: MutableList<CustomDrop>,
    overallChance: Float,
    overallPermissions: MutableList<String>,
    val entityType: EntityType
): CustomDropRecipient(drops, overallChance, overallPermissions) {
    override fun getRecipientType(): CustomDropRecipientType {
        return CustomDropRecipientType.ENTITY_TYPE
    }
}