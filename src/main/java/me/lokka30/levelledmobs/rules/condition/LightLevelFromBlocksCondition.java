package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comment.
 */
public record LightLevelFromBlocksCondition(
        int min,
        int max
) implements RuleCondition {

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity, @NotNull LevelledMobs main) {
        final byte lightLevel = livingEntity.getLocation().getBlock().getLightFromBlocks();
        return lightLevel >= min && lightLevel <= max;
    }
}
