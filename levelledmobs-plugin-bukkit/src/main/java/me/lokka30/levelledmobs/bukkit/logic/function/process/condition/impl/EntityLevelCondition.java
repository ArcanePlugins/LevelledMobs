package me.lokka30.levelledmobs.bukkit.logic.function.process.condition.impl;

import java.util.Locale;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.logic.levelling.LevelledState;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.math.RangedInt;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EntityLevelCondition extends Condition {

    private final LevelledState levelledState;
    private RangedInt requiredLevelRange;         // Compiler won't let us use final here. Whatever.

    public EntityLevelCondition(
        Process parentProcess,
        @NotNull CommentedConfigurationNode conditionNode
    ) {
        super(parentProcess, conditionNode);

        if(getConditionNode().hasChild("state")) {
            final String stateStr = getConditionNode().node("state").getString("EITHER");
            try {
                levelledState = LevelledState.valueOf(stateStr.toUpperCase(Locale.ROOT));
            } catch(IllegalArgumentException ex) {
                Log.war("Invalid levelled state '" + stateStr + "'!", true);
                throw new RuntimeException(ex);
            }
        } else {
            levelledState = LevelledState.EITHER;
        }

        if(levelledState != LevelledState.NOT_LEVELLED) {
            if (getConditionNode().hasChild("range")) {
                requiredLevelRange = new RangedInt(
                    getConditionNode().node("range").getString("")
                );
            }
        }
    }

    @Override
    public boolean applies(Context context) {
        final Entity entity = context.getEntity();
        if(entity == null) return false;
        if(!(entity instanceof LivingEntity lent)) return false;

        final boolean isLevelled = EntityDataUtil.isLevelled(lent, true);

        if(getLevelledState() == LevelledState.NOT_LEVELLED) return !isLevelled;
        if(getLevelledState() == LevelledState.LEVELLED) {
            if(!isLevelled) return false;

            if(getRequiredLevelRange() != null) return true;

            //noinspection ConstantConditions
            return getRequiredLevelRange().contains(EntityDataUtil.getLevel(lent, true));
        }

        return true;
    }

    @NotNull
    public LevelledState getLevelledState() {
        return levelledState;
    }

    @Nullable
    public RangedInt getRequiredLevelRange() {
        return requiredLevelRange;
    }
}
