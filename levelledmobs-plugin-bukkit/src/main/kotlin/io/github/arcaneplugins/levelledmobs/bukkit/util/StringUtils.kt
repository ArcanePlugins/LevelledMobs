package io.github.arcaneplugins.levelledmobs.bukkit.util

import java.util.function.Supplier

object StringUtils {
    fun replaceIfExists(
        str: String,
        target: String,
        operation: Supplier<Any>
    ): String {
        return if (str.contains(target)) {
            str.replace(target, operation.get().toString())
        }
        else str
    }

    fun emptyIfNull(
        str: String?
    ): String {
        return str ?: " "
    }
}