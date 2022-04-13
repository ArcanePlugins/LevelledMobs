package me.lokka30.levelledmobs.api.bukkit;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

public class LevelKeys {

    private LevelKeys() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    private enum Type {
        LEVEL_KEY("level"),
        MIN_LEVEL_KEY("min-level"),
        MAX_LEVEL_KEY("max-level"),
        SPAWN_REASON("spawn-reason"),
        FROZEN_LEVEL_STATE("frozen-level-stage");

        Type(final String key) {
            this.key = key;
            this.nsKey = new NamespacedKey(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("LevelledMobs"),
                    "plugin"),
                key
            );
        }

        private final String key;

        public String getKey() {
            return key;
        }

        private final NamespacedKey nsKey;
    }

}
