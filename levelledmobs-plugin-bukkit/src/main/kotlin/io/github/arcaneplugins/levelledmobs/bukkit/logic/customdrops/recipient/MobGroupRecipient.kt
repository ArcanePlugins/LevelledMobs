package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop

class MobGroupRecipient(
    drops: MutableList<CustomDrop>,
    overallChance: Float,
    overallPermissions: MutableList<String>,
    val mogGroupId: String
): CustomDropRecipient(drops, overallChance, overallPermissions) {
    override fun getRecipientType(): CustomDropRecipientType {
        return CustomDropRecipientType.MOB_GROUP
    }
}