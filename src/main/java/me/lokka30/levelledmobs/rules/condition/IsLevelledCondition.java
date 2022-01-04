package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comment.
 */
public record IsLevelledCondition(
        boolean isLevelled
) implements RuleCondition {

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity, @NotNull LevelledMobs main) {
        return LevelledMob.isLevelled(livingEntity, main) == isLevelled;
    }
}
