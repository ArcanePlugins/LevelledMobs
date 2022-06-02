package me.lokka30.levelledmobs.bukkit.listeners;

import java.util.Set;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;

public final class ListenerHandler {

    /* vars */

    private final Set<ListenerWrapper> listeners = Set.of(
        new EntitySpawnListener(),
        new PlayerJoinListener()
    );

    /* methods */

    public boolean load() {
        Log.inf("Registering listeners.");
        for(var listener : getListeners())
            if(!listener.register())
                return false;
        return true;
    }

    /* getters and setters */

    @NotNull
    public Set<ListenerWrapper> getListeners() { return listeners; }

}
