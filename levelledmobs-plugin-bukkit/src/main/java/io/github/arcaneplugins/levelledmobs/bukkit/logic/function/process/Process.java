package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.preset.Preset;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

@SuppressWarnings("unused")
public class Process {

    /*
    TODO
        - isEnabled
        - factor in isEnabled
     */

    /* vars */

    private final String identifier;
    private final String description;
    private final CommentedConfigurationNode node;
    private final LmFunction parentFunction;
    private final long delay;
    private final Set<Preset> presets = new LinkedHashSet<>();
    private final List<Action> actions = new LinkedList<>();
    private final List<Condition> conditions = new LinkedList<>();
    private boolean exit = false;

    /* constructors */

    public Process(
        final @NotNull String identifier,
        final @NotNull String description,
        final @NotNull CommentedConfigurationNode node,
        final @NotNull LmFunction parentFunction,
        final long delay
    ) {
        this.identifier = identifier;
        this.description = description;
        this.node = node;
        this.parentFunction = parentFunction;
        this.delay = delay;
    }

    /* methods */

    public void call(
        final @NotNull Context context,
        final boolean overrideConditions
    ) {
        final Supplier<Boolean> canRun = () -> overrideConditions || conditionsApply(context);

        if(getDelay() == 0) {
            if(!canRun.get()) return;
            runActions(context);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!canRun.get()) return;
                    runActions(context);
                }
            }.runTaskLater(LevelledMobs.getInstance(), getDelay());
        }
    }

    private boolean conditionsApply(final @NotNull Context context) {
        final int totalConditions = getConditions().size();

        if(totalConditions == 0)
            return true;

        final float conditionsPercentageRequired = 1.0f; //TODO configurable
        int conditionsMet = 0;

        for(var condition : getConditions()) {
            if (condition.applies(context)) {
                conditionsMet++;
            }
        }

        return (conditionsMet * 1.0f / totalConditions) >= conditionsPercentageRequired;
    }

    private void runActions(final @NotNull Context context) {
        exit = false;
        for(var action : getActions()) {
            if(exiting()) return;
            action.run(context);
        }
        exit = false;
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() { return identifier; }

    @NotNull
    public String getDescription() { return description; }

    @NotNull
    public Set<Preset> getPresets() { return presets; }

    @NotNull
    public CommentedConfigurationNode getNode() { return node; }

    @NotNull
    public List<Action> getActions() { return actions; }

    @NotNull
    public List<Condition> getConditions() { return conditions; }

    @NotNull
    public LmFunction getParentFunction() { return parentFunction; }

    public long getDelay() { return delay; }

    public boolean exiting() {
        return exit;
    }

    public void setExiting(final boolean exit) {
        this.exit = exit;
    }

}
