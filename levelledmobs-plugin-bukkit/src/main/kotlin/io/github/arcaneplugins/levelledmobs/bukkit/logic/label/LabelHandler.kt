package io.github.arcaneplugins.levelledmobs.bukkit.logic.label

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

interface LabelHandler{
    val id: String

    fun update(
        context: Context,
        formula: String
    )

//    fun update(
//        lent: LivingEntity,
//        player: Player,
//        context: Context
//    )

    fun deferEntityUpdate(
        entity: LivingEntity,
        context: Context
    )
}