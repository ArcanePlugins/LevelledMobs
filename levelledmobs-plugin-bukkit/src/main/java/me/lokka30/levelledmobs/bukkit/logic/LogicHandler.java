package me.lokka30.levelledmobs.bukkit.logic;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.functions.LmFunction;
import me.lokka30.levelledmobs.bukkit.utils.Log;

public final class LogicHandler {

    /* vars */

    private final Set<LmFunction> functions = new HashSet<>();

    /* methods */

    /*
    Initialisation - parse functions, presets, groups, etc.
     */
    public boolean load() {
        Log.inf("Loading logic system.");
        //TODO
        return true;
    }

    /* getters and setters */

    public Set<LmFunction> getFunctions() { return functions; }

}
