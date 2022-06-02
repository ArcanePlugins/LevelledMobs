package me.lokka30.levelledmobs.bukkit.logic.functions.processes.conditions;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.functions.LmFunction;
import me.lokka30.levelledmobs.bukkit.logic.functions.RunContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class Condition {

    /* vars */

    private final String identifier;
    private final CommentedConfigurationNode node;

    /* constructors */

    public Condition(
        final @NotNull String identifier,
        final @NotNull CommentedConfigurationNode node
    ) {
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        this.node = Objects.requireNonNull(node, "node");
    }

    /* methods */

    public abstract boolean apply(
        final @NotNull LmFunction function,
        final @NotNull RunContext context
    );

    /* getters */

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    public CommentedConfigurationNode getNode() {
        return node;
    }

}
