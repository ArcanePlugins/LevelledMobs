package io.github.arcaneplugins.levelledmobs.bukkit.logic.label

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil
import org.bukkit.entity.LivingEntity

object LabelRegistry {
    val labelHandlers = mutableSetOf<LabelHandler>()

    fun setPrimaryLabelHandler(
        lent: LivingEntity,
        handlerId: String,
        requirePersistence: Boolean
    ) {
        InternalEntityDataUtil.setPrimaryLabelHandler(lent, handlerId, requirePersistence)
    }
}