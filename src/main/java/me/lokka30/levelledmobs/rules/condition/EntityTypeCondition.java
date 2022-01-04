package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.entity.EntityType;
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
    public boolean appliesTo(@NotNull LevelledMob levelledMob) {
        return allowedTypes.contains(levelledMob.getLivingEntity().getType());
    }
}
