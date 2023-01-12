package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetDeathLabelAction extends Action {

    private final String formula;

    public SetDeathLabelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        this.formula = Objects.requireNonNull(
            actionNode.node("formula").getString(),
            "formula"
        );
    }

    @Override
    public void run(Context context) {
        final Entity entity = context.getEntity();

        Objects.requireNonNull(entity, "entity");

        if(!(entity instanceof final LivingEntity lentity)) {
            throw new IllegalArgumentException("Action requires LivingEntity context");
        }

        InternalEntityDataUtil.setDeathLabelFormula(lentity, formula, true);
    }

    @NotNull
    public String getFormula() {
        return formula;
    }
}
