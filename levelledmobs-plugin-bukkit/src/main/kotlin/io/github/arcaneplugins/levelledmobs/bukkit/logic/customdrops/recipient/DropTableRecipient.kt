package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet

class DropTableRecipient(
    val id: String,
    drops: MutableList<CustomDrop>,
    overallChance: Float,
    overallPermissions: MutableList<String>,
    val applicableEntities: ModalEntityTypeSet
) : CustomDropRecipient(drops, overallChance, overallPermissions) {
    override fun getRecipientType(): CustomDropRecipientType {
        return CustomDropRecipientType.DROP_TABLE
    }
}