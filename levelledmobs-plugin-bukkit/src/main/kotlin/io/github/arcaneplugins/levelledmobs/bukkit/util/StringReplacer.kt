package io.github.arcaneplugins.levelledmobs.bukkit.util

import java.util.function.Supplier

class StringReplacer(var text: String) {

    fun replace(placeholders: MutableList<String>, replaceWith: String?) {
        for (text in placeholders){
            replace(text, replaceWith)
        }
    }

    fun replaceIfExists(
        target: String,
        operation: Supplier<Any?>?
    ) {
        val newText = operation?.get()?.toString() ?: ""

        if (text.contains(target, ignoreCase = true)){
            replace(target, newText)
        }
    }

    fun replace(replace: String, replaceWith: Double) {
        replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: Int) {
        replace(replace, replaceWith.toString())
    }

    fun replace(replace: String, replaceWith: String?) {
        val replaceWithText = replaceWith ?: ""
        text = text.replace(replace, replaceWithText, true)
    }
}