package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comments.
 */
public interface RuleCondition {

    @NotNull
    RuleConditionType getType();

    boolean appliesTo(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main);

    @NotNull
    RuleCondition merge(final @NotNull RuleCondition other);
}
