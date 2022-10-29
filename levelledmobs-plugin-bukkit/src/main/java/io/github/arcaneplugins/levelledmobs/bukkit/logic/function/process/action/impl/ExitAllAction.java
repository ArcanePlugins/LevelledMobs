package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ExitAllAction extends Action {

    public ExitAllAction(
        @NotNull Process process,
        @NotNull CommentedConfigurationNode node
    ) {
        super(process, node);
    }

    @Override
    public void run(Context context) {
        getParentProcess().getParentFunction().exitAll(context);
    }
}
