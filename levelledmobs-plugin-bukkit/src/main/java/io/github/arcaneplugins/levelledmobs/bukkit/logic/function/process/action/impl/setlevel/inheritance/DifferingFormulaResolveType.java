package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.inheritance;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

public enum DifferingFormulaResolveType {

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

    @NotNull
    public static DifferingFormulaResolveType getFromAdvancedSettings() {
        return DifferingFormulaResolveType.valueOf(
            LevelledMobs.getInstance().getConfigHandler().getSettingsCfg().getRoot()
                .node("advanced",
                    "set-level-action", "inheritance", "breeding", "differing-formulas")
                .getString(USE_AVERAGE.name())
                .toUpperCase(Locale.ROOT)
        );
    }

}
