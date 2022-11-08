package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.basic.BasicLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.random.RandomLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.spawndistance.SpawnDistanceLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.variable.VariableLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom.WeightedRandomLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.yaxis.YAxisLevellingStrategy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class LevellingStrategyRequestListener extends ListenerWrapper {

    /**
     * Create a new LevellingStrategyRequestListener object.
     */
    public LevellingStrategyRequestListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLevellingStrategyRequest(final LevellingStrategyRequestEvent event) {
        if(event.isClaimed()) return;
        final CommentedConfigurationNode strategyNode = event.getStrategyNode();

        switch(event.getStrategyId()) {
            case "basic" -> {
                event.getStrategies().add(BasicLevellingStrategy.parse(strategyNode));
            }
            case "random" -> {
                event.getStrategies().add(RandomLevellingStrategy.parse(strategyNode));
            }
            case "spawn-distance" -> {
                event.getStrategies().add(SpawnDistanceLevellingStrategy.parse(strategyNode));
            }
            case "variable" -> {
                event.getStrategies().add(VariableLevellingStrategy.parse(strategyNode));
            }
            case "weighted-random" -> {
                event.getStrategies().add(WeightedRandomLevellingStrategy.parse(strategyNode));
            }
            case "y-axis" -> {
                event.getStrategies().add(YAxisLevellingStrategy.parse(strategyNode));
            }
            default -> {
                return;
            }
        }

        event.claim();
    }
}
