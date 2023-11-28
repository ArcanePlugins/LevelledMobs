package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.concurrent.ThreadLocalRandom

class ChanceCondition(
    process: Process,
    node: CommentedConfigurationNode
): Condition(process, node) {
    val chance = node.node("value").getFloat(100.0f)

    override fun applies(context: Context): Boolean {
        val random = ThreadLocalRandom.current().nextFloat()
        val chance: Float = chance / 100.0f
        return random <= chance
    }
}