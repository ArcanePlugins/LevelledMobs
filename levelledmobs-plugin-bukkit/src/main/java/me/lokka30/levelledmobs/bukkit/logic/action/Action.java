package me.lokka30.levelledmobs.bukkit.logic.action;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.Context;
import me.lokka30.levelledmobs.bukkit.logic.Process;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class Action {

    /* vars */

    private final Process process;
    private final CommentedConfigurationNode node;

    /* constructors */

    public Action(final @NotNull Process process, final @NotNull CommentedConfigurationNode node) {
        this.process = Objects.requireNonNull(process, "process");
        this.node = Objects.requireNonNull(node, "node");
    }

    /* methods */

    public abstract void run(final Context context);

    /* getters and setters */

    @NotNull
    public Process getProcess() { return process; }

    @NotNull
    public CommentedConfigurationNode getNode() { return node; }

}
