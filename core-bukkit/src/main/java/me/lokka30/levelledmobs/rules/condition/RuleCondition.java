package me.lokka30.levelledmobs.rules.condition;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comments.
 */
public interface RuleCondition {

    @NotNull
    RuleConditionType getType();

    boolean appliesTo(final @NotNull LivingEntity livingEntity);

    @NotNull
    default RuleCondition merge(final @NotNull RuleCondition other) {
        //TODO
        return this;
    }
}
