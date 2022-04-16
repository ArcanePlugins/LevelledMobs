package me.lokka30.levelledmobs.plugin.bukkit.listener;

import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.util.ClassUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public interface LMListener extends Listener {

    @NotNull
    String getEventClasspath();

    default boolean shouldRegister() {
        return ClassUtils.classExists(getEventClasspath());
    }

    default void register(final @NotNull LevelledMobs main) {
        if(shouldRegister()) {
            Bukkit.getPluginManager().registerEvents(this, main);
        }
    }

}
