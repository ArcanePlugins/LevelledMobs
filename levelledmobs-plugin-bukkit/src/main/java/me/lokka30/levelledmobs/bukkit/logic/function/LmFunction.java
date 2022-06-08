package me.lokka30.levelledmobs.bukkit.logic.function;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

/*
Note: To avoid a naming conflict with Java's 'Function' class, this is named 'LmFunction' (meaning
'LevelledMobs Function').
 */
public class LmFunction {

    /* vars */

    private final String identifier;
    private final String description;
    private final CommentedConfigurationNode node;
    private final Set<String> triggers = new HashSet<>();
    private final Set<Process> processes = new LinkedHashSet<>();

    /* constructors */

    public LmFunction(
        @NotNull final String identifier,
        @NotNull final String description,
        @NotNull final CommentedConfigurationNode node
    ) {
        this.identifier = identifier;
        this.description = description;
        this.node = node;
    }

    /* methods */

    public void run(final @NotNull Context context, final boolean overrideConditions) {
        for(var process : getProcesses()) {
            if(overrideConditions || process.conditionsApply(context)) {
                process.runActions(context);
            }
        }
        // TODO Consider ones like exit-function
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
    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public CommentedConfigurationNode getNode() { return node; }

    @NotNull
    public Set<String> getTriggers() {
        return triggers;
    }

    @NotNull
    public Set<Process> getProcesses() {
        return processes;
    }
}
