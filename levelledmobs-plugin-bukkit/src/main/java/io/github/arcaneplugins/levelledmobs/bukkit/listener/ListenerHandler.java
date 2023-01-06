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
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerDeathListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerJoinListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerQuitListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.PlayerTeleportListener;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.impl.TestListener;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public final class ListenerHandler {

    private ListenerHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    /* vars */

    /**
     * These listeners must be registered earlier.
     */
    private static final Set<ListenerWrapper> PRIMARY_LISTENERS = Set.of(
        new ActionParseListener(),
        new ConditionParseListener(),
        new LevellingStrategyRequestListener(),
        new TestListener()
    );

    /**
     * These listeners must be registered later.
     */
    private static final Set<ListenerWrapper> SECONDARY_LISTENERS = Set.of(
        new EntityBreedListener(),
        new EntityDamageByEntityListener(),
        new EntityDamageListener(),
        new EntityDeathListener(),
        new EntityExplodeListener(),
        new EntityRegainHealthListener(),
        new EntitySpawnListener(),
        new EntityTransformListener(),
        new PlayerDeathListener(),
        new PlayerJoinListener(),
        new PlayerQuitListener(),
        new PlayerTeleportListener()
    );

    /* methods */

    public static void loadPrimary() {
        Log.inf("Registering primary listeners");
        getPrimaryListeners().forEach(ListenerWrapper::register);
    }

    public static void loadSecondary() {
        Log.inf("Registering secondary listeners");
        getSecondaryListeners().forEach(ListenerWrapper::register);
    }

    /* getters and setters */

    @NotNull
    public static Set<ListenerWrapper> getPrimaryListeners() { return PRIMARY_LISTENERS; }

    @NotNull
    public static Set<ListenerWrapper> getSecondaryListeners() { return SECONDARY_LISTENERS; }

}
