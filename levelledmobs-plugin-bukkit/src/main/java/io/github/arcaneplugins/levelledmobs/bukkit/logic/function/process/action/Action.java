package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import java.util.Objects;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class Action {

    /* vars */

    private final Process parentProcess;
    private final CommentedConfigurationNode actionNode;

    /* constructors */

    public Action(final @NotNull Process parentProcess, final @NotNull CommentedConfigurationNode actionNode) {
        this.parentProcess = Objects.requireNonNull(parentProcess, "process");
        this.actionNode = Objects.requireNonNull(actionNode, "node");
    }

    /* methods */

    public abstract void run(final Context context);

    /* getters and setters */

    @NotNull
    public Process getParentProcess() { return parentProcess; }

    @NotNull
    public CommentedConfigurationNode getActionNode() { return actionNode; }

}