package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context

interface ContextPlaceholder {
    fun replace(from: String, context: Context): String
}