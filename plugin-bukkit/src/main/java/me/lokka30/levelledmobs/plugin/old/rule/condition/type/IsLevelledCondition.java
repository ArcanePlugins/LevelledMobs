package me.lokka30.levelledmobs.plugin.old.rule.condition.type;

import de.leonhard.storage.sections.FlatFileSection;
import me.lokka30.levelledmobs.plugin.old.level.LevelledMob;
import me.lokka30.levelledmobs.plugin.old.rule.Rule;
import me.lokka30.levelledmobs.plugin.old.rule.condition.DefaultRuleConditionType;
import me.lokka30.levelledmobs.plugin.old.rule.condition.RuleCondition;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record IsLevelledCondition(
    @NotNull Rule parentRule,
    @NotNull State state,
    boolean inverse
) implements RuleCondition {

    @Override
    @NotNull
    public String id() {
        return DefaultRuleConditionType.IS_LEVELLED.id();
    }

    @Override
    public boolean appliesTo(@NotNull LivingEntity livingEntity) {
        boolean isLevelled = LevelledMob.isEntityLevelled(livingEntity);
        boolean applies;
        switch(state) {
            case ANY:
                return !inverse();
            case YES:
                return inverse() != isLevelled;
            case NO:
                return inverse() == isLevelled;
            default:
                throw new IllegalStateException("Unexpected state " + state);
        }
    }

    @Override
    @NotNull
    public RuleCondition merge(@NotNull RuleCondition other) {
        return this; //TODO
    }

    @NotNull
    public static IsLevelledCondition of(final @NotNull Rule parentRule,
        final @NotNull FlatFileSection section) {
        //TODO
        return new IsLevelledCondition(
            parentRule,
            null,
            section.get(".inverse", false)
        );
    }

    public enum State {
        // the mob can be levelled or not levelled
        ANY,

        // the mob must be levelled
        YES,

        // the mob must not be levelled
        NO
    }
}
