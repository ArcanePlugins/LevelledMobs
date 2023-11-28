package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.basic

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import org.spongepowered.configurate.CommentedConfigurationNode
import kotlin.math.floor

class BasicLevellingStrategy(
    val formula: String,
    minLevel: Int,
    maxLevel: Int
): LevellingStrategy("Basic", minLevel, maxLevel) {
    override fun generate(context: Context): Int? {
        return floor(
            evaluateExpression(replacePapiAndContextPlaceholders(formula, context))
        ).toInt()
    }

    override fun replaceInFormula(formula: String, context: Context): String {
        val placeholder = "%basic-level%"
        if (!formula.contains(placeholder)) return formula
        val generatedLevel = generate(context) ?: return formula
        return formula.replace(placeholder, generatedLevel.toString())
    }

    companion object{
        fun parse(
            node: CommentedConfigurationNode
        ): BasicLevellingStrategy{
            return BasicLevellingStrategy(
                node.node("formula").string!!,
                node.node("min-level").int,
                node.node("max-level").int
            )
        }
    }
}