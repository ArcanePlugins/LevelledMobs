package me.lokka30.levelledmobs.bukkit.listener.action;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class TestAction extends Action {

    /* constructors */

    public TestAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
    }

    /* methods */

    @Override
    public void run(Context context) {
        //TODO Remove debug
        Log.inf("DEBUG: Running '__test' action");
    }
}
