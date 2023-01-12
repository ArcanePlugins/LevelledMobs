package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class SetDropTablesAction extends Action {

    private final Set<String> dropTablesToSet = new HashSet<>();

    public SetDropTablesAction(
        @NotNull final Process parentProcess,
        @NotNull final CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        try {
            getDropTablesToSet().addAll(
                actionNode.node("ids")
                    .getList(String.class, Collections.emptyList())
            );
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(
        @NotNull final Context context
    ) {
        final Entity entity = context.getEntity();
        if(entity == null)
            throw new IllegalArgumentException("Action requires entity context");

        if(!(entity instanceof final LivingEntity lentity))
            throw new IllegalArgumentException("Action requires LivingEntity context");

        if(!EntityDataUtil.isLevelled(lentity, true))
            throw new IllegalArgumentException("Action requires levelled mob context");

        InternalEntityDataUtil.setDropTableIds(lentity, getDropTablesToSet());
    }

    public @NotNull Set<String> getDropTablesToSet() {
        return dropTablesToSet;
    }
}
