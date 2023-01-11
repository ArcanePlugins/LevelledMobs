package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetDeathLabelAction extends Action {

    public SetDeathLabelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
        Log.inf("SetDeathLabelAction will not be parsed: implementation incomplete.");
        //TODO
    }

    @Override
    public void run(Context context) {
        //TODO implement
    }
}
