package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comment.
 */
public interface RuleCondition {

    /*
    TODO:
        - Add javadoc comment.
     */
    boolean appliesTo(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main);
}
