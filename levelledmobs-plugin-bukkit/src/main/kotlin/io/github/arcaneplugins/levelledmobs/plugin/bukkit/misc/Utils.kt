package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

class Utils {
    companion object {
        fun isInteger(str: String?): Boolean {
            return if (str.isNullOrEmpty()) {
                false
            } else try {
                str.toInt()
                true
            } catch (ex: NumberFormatException) {
                false
            }
        }
    }
}