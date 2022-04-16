package me.lokka30.levelledmobs.plugin.old.rule.condition;

import me.lokka30.levelledmobs.plugin.old.rule.Rule;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface RuleCondition {

    @NotNull
    String id();

    @NotNull
    Rule parentRule();

    boolean inverse();

    boolean appliesTo(final @NotNull LivingEntity livingEntity);

    @NotNull
    RuleCondition merge(final @NotNull RuleCondition other);
}
