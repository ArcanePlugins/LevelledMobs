package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EntityOwnerCondition extends Condition {

    public EntityOwnerCondition(
        Process parentProcess,
        @NotNull CommentedConfigurationNode conditionNode
    ) {
        super(parentProcess, conditionNode);
        //TODO parse modal list
    }

    @Override
    public boolean applies(Context context) {
        final Entity entity = context.getEntity();
        if(entity == null) {
            throw new IllegalStateException("Missing entity context");
        }
        if(!(entity instanceof LivingEntity lent)) {
            throw new IllegalStateException("Entity context is not a LivingEntity type");
        }

        //TODO check integrations

        return true;
    }
}
