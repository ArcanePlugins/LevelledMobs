package io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

/*
FIXME Comment
 */
abstract class KeyStore {

    /*
    FIXME Comment
     */
    static NamespacedKey getKey(final String key) {
        return new NamespacedKey(Objects.requireNonNull(
            Bukkit.getPluginManager().getPlugin("LevelledMobs")
        ), key.toLowerCase(Locale.ROOT));
    }

}
