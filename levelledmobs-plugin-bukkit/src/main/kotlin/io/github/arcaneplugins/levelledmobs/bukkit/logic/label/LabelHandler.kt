package io.github.arcaneplugins.levelledmobs.bukkit.logic.label

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.getLabelHandlerFormulaMap
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

abstract class LabelHandler(
    val id: String
) {
    fun getFormula(
        lent: LivingEntity
    ): String {
        return getLabelHandlerFormulaMap(lent, false)
            .getOrDefault(id, "")
    }

    abstract fun update(
        lent: LivingEntity,
        context: Context
    )

    abstract fun update(
        lent: LivingEntity,
        player: Player,
        context: Context
    )

    fun deferEntityUpdate(
        entity: LivingEntity,
        context: Context
    ){
        //TODO customisable range. may want to use a more efficient nearby entities method too
        entity
            .getNearbyEntities(50.0, 50.0, 50.0)
            .stream()
            .filter { otherEntity: Entity? -> otherEntity is Player }
            .map { player: Entity? -> player as Player? }
            .forEach { player: Player? ->
                update(entity,player!!, context)
            }
    }
}