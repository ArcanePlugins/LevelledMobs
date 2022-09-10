package me.lokka30.levelledmobs.bukkit.util;

public class MathUtils {

    private MathUtils() {}

    public static double round2dp(final double in) {
        return Math.round(in * 100.0d) / 100.0d;
    }

}
