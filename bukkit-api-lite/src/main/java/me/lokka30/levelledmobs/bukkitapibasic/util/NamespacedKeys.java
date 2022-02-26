package me.lokka30.levelledmobs.bukkitapibasic.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public class NamespacedKeys {

    private NamespacedKeys() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    private static final Plugin MAIN = Objects.requireNonNull(
        Bukkit.getPluginManager().getPlugin("LevelledMobs"),
        "LevelledMobs is not installed on the server, or " +
            "is not yet enabled!"
    );
    public static final NamespacedKey LEVEL_KEY = new NamespacedKey(MAIN, "level");
    public static final NamespacedKey MIN_LEVEL_KEY = new NamespacedKey(MAIN, "min-level");
    public static final NamespacedKey MAX_LEVEL_KEY = new NamespacedKey(MAIN, "max-level");
    public static final NamespacedKey SPAWN_REASON_KEY = new NamespacedKey(MAIN, "spawn-reason");
    public static final NamespacedKey WAS_BABY_MOB_KEY = new NamespacedKey(MAIN, "was-baby-mob");
    public static final NamespacedKey OVERRIDDEN_ENTITY_NAME_KEY = new NamespacedKey(MAIN,
        "overriden-entity-name");
    public static final NamespacedKey PLAYER_LEVELLING_ID_KEY = new NamespacedKey(MAIN,
        "player-levelling-id");
    public static final NamespacedKey CHANCE_RULE_ALLOWED_KEY = new NamespacedKey(MAIN,
        "chance-rule-allowed");
    public static final NamespacedKey CHANCE_RULE_DENIED_KEY = new NamespacedKey(MAIN,
        "chance-rule-denied");
    public static final NamespacedKey NAMETAG_FORMAT_KEY = new NamespacedKey(MAIN,
        "nametag-format");
    public static final NamespacedKey MAJOR_PLUGIN_VERSION_KEY = new NamespacedKey(MAIN,
        "major-plugin-version");

}
