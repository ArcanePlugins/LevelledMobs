package me.lokka30.levelledmobs.bukkit.api.util;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PluginUtil {

    @NotNull
    public static Plugin getMainInstance() {
        return Objects.requireNonNull(
            Bukkit.getPluginManager().getPlugin("LevelledMobs"),
            "plugin"
        );
    }

}
