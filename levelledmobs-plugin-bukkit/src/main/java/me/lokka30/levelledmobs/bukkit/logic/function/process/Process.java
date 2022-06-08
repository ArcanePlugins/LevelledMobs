package me.lokka30.levelledmobs.bukkit.logic.function.process;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.LmFunction;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.logic.preset.Preset;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Process {

    /* vars */

    private final String identifier;
    private final String description;
    private final CommentedConfigurationNode node;
    private final LmFunction parentFunction;
    private final Set<Preset> presets = new LinkedHashSet<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();
    private boolean exit = false;

    /* constructors */

    public Process(
        final @NotNull String identifier,
        final @NotNull String description,
        final @NotNull CommentedConfigurationNode node,
        final @NotNull LmFunction parentFunction
    ) {
        this.identifier = identifier;
        this.description = description;
        this.node = node;
        this.parentFunction = parentFunction;
    }

    /* methods */

    public boolean conditionsApply(final @NotNull Context context) {
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

    public void runActions(final @NotNull Context context) {
        for(var action : getActions()) {
            if(!shouldExit()) {
                action.run(context);
            }
        }

        setShouldExit(false);
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

    public boolean shouldExit() {
        return exit;
    }

    public void setShouldExit(final boolean state) {
        this.exit = state;
    }

}
