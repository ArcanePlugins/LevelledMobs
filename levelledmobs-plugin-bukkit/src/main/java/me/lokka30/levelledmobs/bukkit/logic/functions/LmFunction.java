package me.lokka30.levelledmobs.bukkit.logic.functions;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.functions.processes.Process;
import me.lokka30.levelledmobs.bukkit.logic.functions.triggers.Trigger;
import org.jetbrains.annotations.NotNull;

/*
Note: To avoid a naming conflict with Java's 'Function' class, this is named 'LmFunction' (meaning
'LevelledMobs Function').
 */
public class LmFunction {

    /* vars */
    private final String id;
    private final String description;
    private final Set<Trigger> triggers = new HashSet<>();
    private final Set<Process> processes = new HashSet<>();

    /* constructors */

    public LmFunction(
        @NotNull final String id,
        @NotNull final String description,
        @NotNull final Set<Trigger> triggers,
        @NotNull final Set<Process> processes
    ) {
        this.id = id;
        this.description = description;
        this.triggers.addAll(triggers);
        this.processes.addAll(processes);
    }

    /*
    getters and setters
    */

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public Set<Trigger> getTriggers() {
        return triggers;
    }

    @NotNull
    public Set<Process> getProcesses() {
        return processes;
    }
}
