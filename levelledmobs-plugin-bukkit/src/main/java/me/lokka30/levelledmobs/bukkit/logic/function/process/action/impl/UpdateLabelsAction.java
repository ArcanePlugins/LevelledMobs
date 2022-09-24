package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
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

        assert context.getEntity() != null;

        InternalEntityDataUtil
            .updateLabels((LivingEntity) context.getEntity(), context, false);

    }
}
