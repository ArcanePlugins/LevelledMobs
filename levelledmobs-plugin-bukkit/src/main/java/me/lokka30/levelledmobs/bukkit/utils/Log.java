package me.lokka30.levelledmobs.bukkit.utils;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;

@SuppressWarnings("unused")
public final class Log {

    private Log() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    public static void inf(final String msg) {
        LevelledMobs.getInstance().getLogger().info(msg);
    }

    public static void war(final String msg) {
        LevelledMobs.getInstance().getLogger().warning(msg);
    }

    public static void sev(final String msg) {
        LevelledMobs.getInstance().getLogger().severe(msg);
    }

}
