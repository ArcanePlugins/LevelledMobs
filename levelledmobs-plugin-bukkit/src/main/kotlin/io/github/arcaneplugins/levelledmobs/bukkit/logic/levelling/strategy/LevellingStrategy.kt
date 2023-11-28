package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context

abstract class LevellingStrategy(
    val name: String,
    val minLevel: Int,
    val maxLevel: Int
) {
    abstract fun generate(context: Context): Int?

    /*
    replaces placeholders like %random-level% or whatever the strategy chooses with the generated
    level for a mob with given context.
     */
    abstract fun replaceInFormula(formula: String, context: Context): String
}