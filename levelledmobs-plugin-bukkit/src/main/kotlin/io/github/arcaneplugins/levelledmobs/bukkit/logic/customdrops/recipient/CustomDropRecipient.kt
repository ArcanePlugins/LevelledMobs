package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop

abstract class CustomDropRecipient(
    val drops: MutableList<CustomDrop>,
    val overallChance: Float,
    val overallPermissions: MutableList<String>
) {
    abstract fun getRecipientType() : CustomDropRecipientType
}