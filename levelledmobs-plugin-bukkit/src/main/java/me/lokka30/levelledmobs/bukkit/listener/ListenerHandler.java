package me.lokka30.levelledmobs.bukkit.listener;

import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.impl.ConditionParseListener;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;

public final class ListenerHandler {

    /* vars */

    /**
     * These listeners must be registered earlier.
     */
    private final Set<ListenerWrapper> primaryListeners = Set.of(
        new ActionParseListener(),
        new ConditionParseListener(),
        new LevellingStrategyRequestListener(),
        new TestListener()
    );

    /**
     * These listeners must be registered later.
     */
    private final Set<ListenerWrapper> secondaryListeners = Set.of(
        new EntitySpawnListener(),
        new PlayerJoinListener()
    );

    /* methods */

    public boolean loadPrimary() {
        Log.inf("Registering primary listeners");
        for(var listener : getPrimaryListeners())
            if(!listener.register())
                return false;
        return true;
    }

    public boolean loadSecondary() {
        Log.inf("Registering secondary listeners");
        for(var listener : getSecondaryListeners())
            if(!listener.register())
                return false;
        return true;
    }

    /* getters and setters */

    @NotNull
    public Set<ListenerWrapper> getPrimaryListeners() { return primaryListeners; }

    @NotNull
    public Set<ListenerWrapper> getSecondaryListeners() { return secondaryListeners; }

}
