package me.lokka30.levelledmobs.rules.condition.type;

import de.leonhard.storage.sections.FlatFileSection;
import me.lokka30.levelledmobs.rules.Rule;
import me.lokka30.levelledmobs.rules.condition.DefaultRuleConditionType;
import me.lokka30.levelledmobs.rules.condition.RuleCondition;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record LightLevelFromSkyCondition(
        @NotNull Rule   parentRule,
        int             min,
        int             max,
        boolean         inverse
) implements RuleCondition {

    @Override @NotNull
    public String id() {
        return DefaultRuleConditionType.LIGHT_LEVEL_FROM_SKY.id();
    }

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity) {
        final byte lightLevel = livingEntity.getLocation().getBlock().getLightFromSky();
        return inverse != (lightLevel >= min && lightLevel <= max);
    }

    @Override @NotNull
    public RuleCondition merge(@NotNull RuleCondition other) {
        return this; //TODO
    }

    @NotNull
    public static LightLevelFromSkyCondition of(final @NotNull Rule parentRule, final @NotNull FlatFileSection section) {
        //TODO
        return new LightLevelFromSkyCondition(
                parentRule,
                Integer.MIN_VALUE, //TODO
                Integer.MAX_VALUE, //TODO
                section.get(".inverse", false)
        );
    }
}
