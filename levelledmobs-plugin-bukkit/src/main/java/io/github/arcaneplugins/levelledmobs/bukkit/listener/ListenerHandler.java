package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.ActionParseListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.ConditionParseListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityBreedListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityDamageByEntityListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityDamageListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityDeathListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityExplodeListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityRegainHealthListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntitySpawnListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.EntityTransformListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.LevellingStrategyRequestListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerJoinListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerQuitListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerTeleportListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.TestListener;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Set;
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
        new EntityBreedListener(),
        new EntityDamageByEntityListener(),
        new EntityDamageListener(),
        new EntityDeathListener(),
        new EntityExplodeListener(),
        new EntityRegainHealthListener(),
        new EntitySpawnListener(),
        new EntityTransformListener(),
        new PlayerJoinListener(),
        new PlayerQuitListener(),
        new PlayerTeleportListener()
    );

    /* methods */

    public void loadPrimary() {
        Log.inf("Registering primary listeners");
        getPrimaryListeners().forEach(ListenerWrapper::register);
    }

    public void loadSecondary() {
        Log.inf("Registering secondary listeners");
        getSecondaryListeners().forEach(ListenerWrapper::register);
    }

    /* getters and setters */

    @NotNull
    public Set<ListenerWrapper> getPrimaryListeners() { return primaryListeners; }

    @NotNull
    public Set<ListenerWrapper> getSecondaryListeners() { return secondaryListeners; }

}
