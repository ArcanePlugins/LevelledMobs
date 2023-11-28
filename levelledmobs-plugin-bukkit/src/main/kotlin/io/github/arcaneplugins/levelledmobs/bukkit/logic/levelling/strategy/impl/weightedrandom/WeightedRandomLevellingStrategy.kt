package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.WeightedRandomContainer
import org.spongepowered.configurate.CommentedConfigurationNode

class WeightedRandomLevellingStrategy(
    val levelWeightContainer: WeightedRandomContainer<RangedInt>,
    minLevel: Int,
    maxLevel: Int
): LevellingStrategy("Weighted Random", minLevel, maxLevel) {
    override fun generate(
        context: Context
    ): Int {
        return levelWeightContainer
            .choose() // choose a weighted ranged int
            .choose() // choose a random value in between the min-max ranged int
    }

    override fun replaceInFormula(
        formula: String,
        context: Context
    ): String {
        val placeholder = "%weighted-random-level%"

        return if (!formula.contains(placeholder)) {
            formula
        } else formula.replace(placeholder, generate(context).toString())
    }

    companion object{
        fun parse(
            node: CommentedConfigurationNode
        ): WeightedRandomLevellingStrategy{
            val levelWeightMap = mutableMapOf<RangedInt, Float>()

            // add entries to level weight map
            node.node("tiers").childrenMap().forEach { (key: Any, value: CommentedConfigurationNode) ->
                val ri = when (key){
                    is Int -> {
                        RangedInt(key)
                    }

                    is String -> {
                        RangedInt(key)
                    }

                    else -> {
                        throw IllegalArgumentException("Unable to parse weighted random map as the entry " +
                                "'$key' is a '${key.javaClass.getName()}', not a String or Integer"
                        )
                    }
                }

                levelWeightMap[ri] = value.float
            }

            // determine min and max level
            var minLevel: Int? = null
            var maxLevel: Int? = null

            for (ri in levelWeightMap.keys) {
                if (minLevel == null || ri.min < minLevel) minLevel = ri.min
                if (maxLevel == null || ri.max > maxLevel) maxLevel = ri.max
            }

            requireNotNull(minLevel) { "Unable to determine min level of weighted random map" }
            requireNotNull(maxLevel) { "Unable to determine max level of weighted random map" }

            // return strategy object
            return WeightedRandomLevellingStrategy(
                WeightedRandomContainer(levelWeightMap),
                minLevel,
                maxLevel
            )
        }
    }
}