package io.github.arcaneplugins.levelledmobs.bukkit.util

import java.util.function.Supplier

class StringReplacer(var text: String) {
    fun replace(placeholders: MutableList<String>, replaceWith: String?) : StringReplacer{
        for (text in placeholders){
            replace(text, replaceWith)
        }

        return this
    }

    fun replaceIfExists(
        target: String,
        operation: Supplier<Any?>?
    ): StringReplacer{
        val newText = operation?.get().toString()

        if (text.contains(target, ignoreCase = true)){
            replace(target, newText)
        }

        return this
    }

    fun replace(replace: String, replaceWith: Double) : StringReplacer{
        return replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: Int) : StringReplacer{
        return replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: String?) : StringReplacer{
        val replaceWithText = replaceWith ?: ""

        text = text.replace(replace, replaceWithText, true)
        return this
    }
}