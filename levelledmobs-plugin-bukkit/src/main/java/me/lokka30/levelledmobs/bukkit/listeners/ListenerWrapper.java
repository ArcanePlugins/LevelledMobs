package me.lokka30.levelledmobs.bukkit.listeners;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.utils.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class ListenerWrapper implements Listener {

    /* vars */

    private final String eventClasspath;
    private final boolean imperative;

    /* constructors */

    public ListenerWrapper(
        final @NotNull String eventClasspath,
        final boolean imperative
    ) {
        this.eventClasspath = Objects.requireNonNull(eventClasspath, "eventClasspath");
        this.imperative = imperative;
    }

    /* methods */

    protected boolean canRegister() {
        return ClassUtils.classExists(getEventClasspath());
    }

    public boolean register() {
        if(!canRegister())
            return false;

        Bukkit.getPluginManager().registerEvents(this, LevelledMobs.getInstance());
        return true;
    }

    /* getters and setters */

    protected @NotNull String getEventClasspath() {
        return eventClasspath;
    }

    public boolean isImperative() { return imperative; }

}
