package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.variable

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import org.spongepowered.configurate.CommentedConfigurationNode

class VariableLevellingStrategy(
    minLevel: Int,
    maxLevel: Int
) : LevellingStrategy("Variable", minLevel, maxLevel) {
    override fun generate(context: Context): Int? {
        TODO("Not yet implemented")
    }

    override fun replaceInFormula(formula: String, context: Context): String {
        TODO("Not yet implemented")
    }

    companion object{
        fun parse(
            node: CommentedConfigurationNode
        ): VariableLevellingStrategy{
            TODO("Not yet implemented")
        }
    }
}