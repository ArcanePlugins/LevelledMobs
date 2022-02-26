package me.lokka30.levelledmobs.rule.action;

import me.lokka30.levelledmobs.rule.Rule;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface RuleAction {

    @NotNull
    String id();

    @NotNull
    Rule parentRule();

    void run(final @NotNull LivingEntity livingEntity);

    void merge(final @NotNull RuleAction other);
}
