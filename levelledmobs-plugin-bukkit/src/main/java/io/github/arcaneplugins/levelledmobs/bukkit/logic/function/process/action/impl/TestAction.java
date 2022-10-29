package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
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
        Log.inf("Test action ran at path: " + getActionNode().path());

        //noinspection ConstantConditions
        Bukkit.broadcastMessage(
            "Movement speed = " +
            ((LivingEntity) context.getEntity())
                .getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)
                .getValue()
        );

        //noinspection ConstantConditions
        Bukkit.broadcastMessage(
            "Max health = " +
                ((LivingEntity) context.getEntity())
                    .getAttribute(Attribute.GENERIC_MAX_HEALTH)
                    .getValue()
        );
    }
}
