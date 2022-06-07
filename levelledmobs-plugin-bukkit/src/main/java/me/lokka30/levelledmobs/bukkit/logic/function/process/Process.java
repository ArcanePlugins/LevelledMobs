package me.lokka30.levelledmobs.bukkit.logic.function.process;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.logic.function.LmFunction;
import me.lokka30.levelledmobs.bukkit.logic.preset.Preset;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class Process {

    /* vars */

    private final String identifier;
    private final String description;
    private final CommentedConfigurationNode node;
    private final LmFunction function;
    private final Set<Preset> presets = new LinkedHashSet<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();

    /* constructors */

    public Process(
        final @NotNull String identifier,
        final @NotNull String description,
        final @NotNull CommentedConfigurationNode node,
        final @NotNull LmFunction function
    ) {
        this.identifier = identifier;
        this.description = description;
        this.node = node;
        this.function = function;
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
    public LmFunction getFunction() { return function; }

}
