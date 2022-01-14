package me.lokka30.levelledmobs.rules.condition.type;

import me.lokka30.levelledmobs.rules.condition.RuleCondition;
import me.lokka30.levelledmobs.rules.condition.RuleConditionType;
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
    public @NotNull RuleConditionType getType() {
        return RuleConditionType.ENTITY_TYPE;
    }

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity) {
        return allowedTypes.contains(livingEntity.getType());
    }
}
