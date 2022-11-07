package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class UpdateLabelsAction extends Action {

    public UpdateLabelsAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
    }

    @Override
    public void run(Context context) {
        if(context.getEntity() instanceof LivingEntity lent) {
            InternalEntityDataUtil.updateLabels(lent, context, false);
        }

        if(context.getPlayer() != null) {
            InternalEntityDataUtil.updateLabels(context.getPlayer(), context, false);
        }
    }
}
