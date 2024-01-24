package io.github.arcaneplugins.levelledmobs.misc

import java.util.function.Supplier

/**
 * Holds a string and provides methods to run
 * various string replacement operations
 *
 * @author stumper66
 * @since 3.13.2
 */
class StringReplacer(var text: String) {
    fun replaceIfExists(target: String, operation: Supplier<String?>?) {
        val newText = operation?.get()

        if (text.contains(target)) {
            replace(target, newText)
        }
    }

    fun replace(replace: String, replaceWith: Double) {
        replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: Int) {
        replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: String?): StringReplacer {
        val replaceWithText = replaceWith ?: ""

        text = text.replace(replace, replaceWithText)
        return this
    }

    val isEmpty: Boolean
        get() = text.isEmpty()


    fun contains(s: CharSequence): Boolean {
        return text.contains(s)
    }
}