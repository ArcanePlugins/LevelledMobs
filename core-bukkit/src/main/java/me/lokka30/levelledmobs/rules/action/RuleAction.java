package me.lokka30.levelledmobs.rules.action;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface RuleAction {
    @NotNull
    RuleActionType getType();

    void run(final @NotNull LivingEntity livingEntity);

    @NotNull
    default RuleAction merge(final @NotNull RuleAction other) {
        //TODO
        return this;
    }
}
