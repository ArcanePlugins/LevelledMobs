package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/*
TODO:
    - Add javadoc comment.
 */
public record EntityTypeCondition(
        EnumSet<EntityType> allowedTypes
) implements RuleCondition {

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity, @NotNull LevelledMobs main) {
        return allowedTypes.contains(livingEntity.getType());
    }
}
