package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.LevelledState
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import org.spongepowered.configurate.CommentedConfigurationNode

class EntityLevelCondition(
    process: Process,
    node: CommentedConfigurationNode
): Condition(process, node) {
    val levelledState: LevelledState
    var requiredLevelRange: RangedInt? = null

    init {
        levelledState = if (conditionNode.hasChild("state")) {
            val stateStr = conditionNode.node("state").getString("EITHER")
            try {
                LevelledState.valueOf(stateStr.uppercase())
            } catch (ex: IllegalArgumentException) {
                war("Invalid levelled state '$stateStr'!", true)
                throw RuntimeException(ex)
            }
        } else {
            LevelledState.EITHER
        }

        if (levelledState != LevelledState.NOT_LEVELLED) {
            if (conditionNode.hasChild("range")) {
                requiredLevelRange = RangedInt(
                    conditionNode.node("range").getString("")
                )
            }
        }
    }

    override fun applies(context: Context): Boolean {
        if (context.livingEntity == null) return false
        val lent = context.livingEntity!!

        val isLevelled = EntityDataUtil.isLevelled(lent, true)

        if (levelledState == LevelledState.NOT_LEVELLED) return !isLevelled
        if (levelledState == LevelledState.LEVELLED) {
            if (!isLevelled) return false
            return if (requiredLevelRange == null)
                true
            else
                requiredLevelRange!!.contains(EntityDataUtil.getLevel(lent,true)!!)
        }

        return true
    }
}