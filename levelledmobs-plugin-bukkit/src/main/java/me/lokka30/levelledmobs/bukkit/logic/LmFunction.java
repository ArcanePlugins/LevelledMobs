package me.lokka30.levelledmobs.bukkit.logic;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/*
Note: To avoid a naming conflict with Java's 'Function' class, this is named 'LmFunction' (meaning
'LevelledMobs Function').
 */
public class LmFunction {

    /* vars */

    private final String id;
    private final String description;
    private final LinkedHashSet<String> triggers = new LinkedHashSet<>();
    private final LinkedHashSet<Process> processes = new LinkedHashSet<>();

    /* constructors */

    public LmFunction(
        @NotNull final String id,
        @NotNull final String description,
        @NotNull final Set<String> triggers,
        @NotNull final Set<Process> processes
    ) {
        this.id = id;
        this.description = description;
        this.triggers.addAll(triggers);
        this.processes.addAll(processes);
    }

    /* methods */

    public void run(final @NotNull Context context) {
        //todo
    }

    public boolean hasAnyTriggers(final @NotNull String... triggersToCheck) {
        for(var trigger : getTriggers()) {
            for(var triggerToCheck : triggersToCheck) {
                if(trigger.equalsIgnoreCase(triggerToCheck))
                    return true;
            }
        }
        return false;
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
    public LinkedHashSet<String> getTriggers() {
        return triggers;
    }

    @NotNull
    public LinkedHashSet<Process> getProcesses() {
        return processes;
    }
}
