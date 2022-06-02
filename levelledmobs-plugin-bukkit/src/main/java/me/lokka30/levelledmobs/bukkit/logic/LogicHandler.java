package me.lokka30.levelledmobs.bukkit.logic;

import java.util.LinkedHashSet;
import me.lokka30.levelledmobs.bukkit.logic.functions.LmFunction;
import me.lokka30.levelledmobs.bukkit.logic.functions.RunContext;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;

public final class LogicHandler {

    /* vars */

    private final LinkedHashSet<LmFunction> functions = new LinkedHashSet<>();

    /* methods */

    /**
    Initialisation - parse functions, presets, groups, etc.
     */
    public boolean load() {
        Log.inf("Loading logic system.");
        //TODO
        return true;
    }

    /**
     * Checks all available functions if they have any of the given triggers, and any applicable
     * functions will be ran using the given context.
     *
     * @param context the context given for the function to run with
     * @param triggers a list of trigger IDs, which a function requires at least one match to apply
     */
    public void runFunctionsWithTriggers(
        final @NotNull RunContext context,
        final @NotNull String... triggers
    ) {
        for(var function : getFunctions())
            if(function.hasAnyTriggers(triggers))
                function.run(context);
    }

    /* getters and setters */

    @NotNull
    public LinkedHashSet<LmFunction> getFunctions() { return functions; }

}
