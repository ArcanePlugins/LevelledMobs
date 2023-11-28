package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.random

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.concurrent.ThreadLocalRandom

class RandomLevellingStrategy(
    minLevel: Int,
    maxLevel: Int
): LevellingStrategy("Random", minLevel, maxLevel) {
    /*
    `Integer` (object instead of primitive) is being used here
    as it's possible that other levelling strategies aren't able to generate a
    level for a mob at a context or perhaps user error in the config.
    This particular levelling strategy will always return a non-null integer.
    */
    override fun generate(context: Context): Int {
        return ThreadLocalRandom.current().nextInt(minLevel, maxLevel)
    }

    override fun replaceInFormula(
        formula: String,
        context: Context
    ): String {
        val placeholder = "%random-level%"

        return if (!formula.contains(placeholder)) {
            formula
        }
        else formula.replace(placeholder, generate(context).toString())
    }

    companion object{
        fun parse(
            node: CommentedConfigurationNode
        ): RandomLevellingStrategy{
            /* error checking */
            val declaresMinLevel = node.hasChild("min-level")
            val declaresMaxLevel = node.hasChild("max-level")
            var whatToFix: String? = null
            if (!declaresMaxLevel && !declaresMinLevel) {
                whatToFix = "min and max"
            } else if (!declaresMinLevel) {
                whatToFix = "min"
            } else if (!declaresMaxLevel) {
                whatToFix = "max"
            }

            if (whatToFix != null) {
                sev("""
                Detected an invalid configuration for a Random Levelling strategy: you didn't specify a $whatToFix level. The strategy can't be parsed until this is fixed.
                """.trimIndent(),true)
                throw IllegalStateException()
            }

            /* looks good, let's parse it */
            /* TODO if confirmed working then use this smaller one instead.
            return new RandomLevellingStrategy(
                node.node("min-level").getInt(),
                node.node("max-level").getInt()
            );
             */

            val minLevel = node.node("min-level").int
            val maxLevel = node.node("max-level").int

            return RandomLevellingStrategy(minLevel, maxLevel)
        }
    }
}