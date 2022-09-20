package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import java.util.Collection;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ApplyInheritedLevel extends Action {

    public ApplyInheritedLevel(
        final @NotNull Process parentProcess,
        final @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
    }

    @Override
    public void run(Context context) {
        final Entity entity = context.getEntity();
        if(entity == null) throw new IllegalStateException(
            "ApplyInheritanceAction ran without an Entity context"
        );
        if(!(entity instanceof LivingEntity lent)) throw new IllegalStateException(
            "ApplyInheritanceAction ran without an Entity (as LivingEntity) context"
        );
        final Collection<LivingEntity> parents = context.getParents();

        final InheritanceType it = InheritanceType.fromParentsSize(parents.size());
        if(it == null) return;

        Integer calculatedLevel = null;

        switch(it) {
            case TRANSFORMATION -> {
                //TODO
                calculatedLevel = 1; //TODO
            }

            case BREEDING -> {
                //TODO
                calculatedLevel = 2; //TODO
            }
        }

        if(calculatedLevel == null) return;

        InternalEntityDataUtil.setLevel(lent, calculatedLevel, true);
    }

    @Nullable
    private Integer calculateBreedLevel(
        final @NotNull LivingEntity child,
        final @NotNull LivingEntity parent1,
        final @NotNull LivingEntity parent2
    ) {
        //TODO
        return null;
    }

    private enum InheritanceType {
        TRANSFORMATION(1),
        BREEDING(2);

        private final int parentsSize;

        InheritanceType(
            final int parentsSize
        ) {
            this.parentsSize = parentsSize;
        }

        public int getParentsSize() {
            return parentsSize;
        }

        @Nullable
        public static InheritanceType fromParentsSize(final int parentsSize) {
            for(final InheritanceType it : values()) {
                if(it.getParentsSize() == parentsSize) {
                    return it;
                }
            }
            return null;
        }
    }
}
