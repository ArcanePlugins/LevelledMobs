package me.lokka30.levelledmobs.rules.condition.type;

import me.lokka30.levelledmobs.level.LevelledMob;
import me.lokka30.levelledmobs.rules.condition.RuleCondition;
import me.lokka30.levelledmobs.rules.condition.RuleConditionType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comment.
 */
public record IsLevelledCondition(
        boolean               isLevelled
) implements RuleCondition {

    @Override
    public @NotNull RuleConditionType getType() {
        return RuleConditionType.IS_LEVELLED;
    }

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity) {
        return LevelledMob.isEntityLevelled(livingEntity) == isLevelled;
    }
}
