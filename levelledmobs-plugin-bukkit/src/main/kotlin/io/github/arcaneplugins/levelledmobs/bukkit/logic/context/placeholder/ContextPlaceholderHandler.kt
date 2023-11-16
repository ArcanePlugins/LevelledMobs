package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl.StandardPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringUtils
import org.bukkit.Bukkit

class ContextPlaceholderHandler {
    val contextPlaceholders = mutableSetOf<ContextPlaceholder>()

    fun load(){
        contextPlaceholders.clear()
        contextPlaceholders.add(StandardPlaceholders())
        Bukkit.getPluginManager().callEvent(ContextPlaceholdersLoadEvent())
    }

    fun replace(
        input: String,
        context: Context
    ): String{
        var updatedInput = input
        for (placeholder in contextPlaceholders) {
            updatedInput = placeholder.replace(updatedInput, context)
        }

        for (entry in context.miscContext.entries) {
            val placeholder: String = entry.key
            val supplier = entry.value
            updatedInput = StringUtils.replaceIfExists(updatedInput, placeholder, supplier)
        }

        return updatedInput
    }
}