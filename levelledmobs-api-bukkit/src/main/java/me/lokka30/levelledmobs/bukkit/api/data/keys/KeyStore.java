package me.lokka30.levelledmobs.bukkit.api.data.keys;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

abstract class KeyStore {

    private static final Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelledMobs");

    static NamespacedKey getKey(final String key) {
        return new NamespacedKey(Objects.requireNonNull(plugin), key.toLowerCase(Locale.ROOT));
    }

}
