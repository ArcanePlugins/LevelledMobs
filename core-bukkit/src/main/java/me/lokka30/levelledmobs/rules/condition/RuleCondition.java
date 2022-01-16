package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.rules.Rule;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface RuleCondition {

    @NotNull
    RuleConditionType type();

    @NotNull
    Rule parentRule();

    boolean inverse();

    boolean appliesTo(final @NotNull LivingEntity livingEntity);

    @NotNull
    default RuleCondition merge(final @NotNull RuleCondition other) {
        //TODO
        return this;
    }
}
