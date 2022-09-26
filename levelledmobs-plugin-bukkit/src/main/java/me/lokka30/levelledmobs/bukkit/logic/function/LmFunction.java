package me.lokka30.levelledmobs.bukkit.logic.function;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

/*
Note: To avoid a naming conflict with Java's 'Function' class, this is named 'LmFunction' (meaning
'LevelledMobs Function').
 */
@SuppressWarnings("unused")
public class LmFunction {

    /*
    TODO
        - isEnabled
        - factor in isEnabled
     */

    /* vars */

    private final String identifier;
    private final String description;
    private final CommentedConfigurationNode node;
    private final Set<String> triggers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<Process> processes = new LinkedHashSet<>();
    private boolean exit = false;

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
            if(exiting()) {
                this.exit = false;
                return;
            }

            if(overrideConditions || process.conditionsApply(context)) {
                process.runActions(context);
            }
        }
    }

    public boolean hasAnyTriggers(final @NotNull String... triggersToCheck) {
        for(var triggerToCheck : triggersToCheck) {
            if(getTriggers().contains(triggerToCheck)){
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

    /**
     * <b>WARNING:</b> This node is a preset-parsed object copy, it does not reflect the actual
     * node present in settings.yml. Modifying this node will not make any change to settings.yml!
     *
     * @return root config node of the function
     */
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

    public boolean exiting() {
        return exit;
    }

    public void setExiting(final boolean exit) {
        this.exit = exit;
        getProcesses().forEach(process -> process.setExiting(exit));
    }

    public void exitAll(final @Nonnull Context context) {
        setExiting(true);
        context.getLinkedFunctions().forEach(lmFunction -> lmFunction.setExiting(true));
    }
}
