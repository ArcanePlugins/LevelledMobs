package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ExitFunctionAction extends Action {

    public ExitFunctionAction(
        @NotNull Process process,
        @NotNull CommentedConfigurationNode node
    ) {
        super(process, node);
    }

    @Override
    public void run(Context context) {
        getParentProcess().getParentFunction().setShouldExit(true);
        getParentProcess().setShouldExit(true);
    }
}
