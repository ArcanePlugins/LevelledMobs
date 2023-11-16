package io.github.arcaneplugins.levelledmobs.bukkit.util.math

object MathUtils {
    fun round2dp(dbl: Double): Double {
        return Math.round(dbl * 100.0) / 100.0
    }
}