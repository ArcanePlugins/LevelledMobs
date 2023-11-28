package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.basic.BasicLevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.random.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.spawndistance.SpawnDistanceLevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.variable.VariableLevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom.WeightedRandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.yaxis.YAxisLevellingStrategy
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class LevellingStrategyRequestListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onLevellingStrategyRequest(event: LevellingStrategyRequestEvent){
        if (event.claimed) return
        val strategyNode = event.strategyNode

        when (event.strategyId) {
            "basic" -> {
                event.strategies.add(BasicLevellingStrategy.parse(strategyNode))
            }
            "random" -> {
                event.strategies.add(RandomLevellingStrategy.parse(strategyNode))
            }
            "spawn-distance" -> {
                event.strategies.add(SpawnDistanceLevellingStrategy.parse(strategyNode))
            }
            "variable" -> {
                event.strategies.add(VariableLevellingStrategy.parse(strategyNode))
            }
            "weighted-random" -> {
                event.strategies.add(WeightedRandomLevellingStrategy.parse(strategyNode))
            }
            "y-axis" -> {
                event.strategies.add(YAxisLevellingStrategy.parse(strategyNode))
            }
            else -> {
                return
            }
        }

        event.claimed = true
    }
}