package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.inheritance

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs

enum class DifferingFormulaResolveType {

    /**
     * TODO Document
     * to use both and average the result
     */
    USE_AVERAGE,

    /**
     * TODO Document
     * to pick either randomly
     */
    USE_RANDOM,

    /**
     * TODO Document
     * to skip any inheritance logic and proceed normally
     */
    USE_NEITHER
            ;

    companion object{
        fun getFromAdvancedSettings(): DifferingFormulaResolveType{
            return valueOf(
                LevelledMobs.lmInstance.configHandler.settingsCfg.root!!
                    .node(
                        "advanced",
                        "set-level-action", "inheritance", "breeding", "differing-formulas"
                    )
                    .getString(USE_AVERAGE.name)
                    .uppercase()
            )
        }
    }
}