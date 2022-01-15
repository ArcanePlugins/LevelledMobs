package me.lokka30.levelledmobs.rules.condition.type;

import de.leonhard.storage.sections.FlatFileSection;
import me.lokka30.levelledmobs.level.LevelledMob;
import me.lokka30.levelledmobs.rules.condition.RuleCondition;
import me.lokka30.levelledmobs.rules.condition.RuleConditionType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record IsLevelledCondition(
        @NotNull State  state,
        boolean         inverse
) implements RuleCondition {

    @Override
    public @NotNull RuleConditionType getType() {
        return RuleConditionType.IS_LEVELLED;
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

    @NotNull
    public static IsLevelledCondition of(final @NotNull FlatFileSection section) {
        //TODO
        return null;
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
