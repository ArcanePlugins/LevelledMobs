package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.random.RandomLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy.impl.weightedrandom.WeightedRandomLevellingStrategy;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

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

        Log.tmpdebug("Listener caught LevellingStrategyRequestEvent");

        switch(event.getStrategyId()) {
            case "player" -> {
                //TODO Add static parsing method into PlayerLevellingStrategy
                Log.sev("player levelling parsing not implemented", false);
            }
            case "random" -> {
                event.getStrategies().add(RandomLevellingStrategy.parse(event.getStrategyNode()));
            }
            case "spawn-distance" -> {
                //TODO Add static parsing method into SpawnDistanceStrategy
                Log.sev("spawn distance levelling parsing not implemented", false);
            }
            case "weighted-random" -> {
                event.getStrategies().add(WeightedRandomLevellingStrategy.parse(event.getStrategyNode()));
            }
            case "y-coord" -> {
                //TODO Add static parsing method into YCoordStrategy
                Log.sev("y coordinate levelling parsing not implemented", false);
            }
            default -> {
                return;
            }
        }

        event.claim();
    }
}
