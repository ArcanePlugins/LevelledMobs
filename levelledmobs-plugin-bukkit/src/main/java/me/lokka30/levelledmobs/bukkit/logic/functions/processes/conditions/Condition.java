package me.lokka30.levelledmobs.bukkit.logic.functions.processes.conditions;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class Condition {

    /* vars */

    private final String id;
    private final CommentedConfigurationNode node;

    /* constructors */

    public Condition(
        final @NotNull String id,
        final @NotNull CommentedConfigurationNode node
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.node = Objects.requireNonNull(node, "node");
    }

    //TODO

}
