package me.lokka30.levelledmobs.rules.action.type;

import me.lokka30.levelledmobs.rules.action.RuleAction;
import me.lokka30.levelledmobs.rules.action.RuleActionType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class ExecuteAction implements RuleAction {

    //TODO javadocs
    @Override
    public @NotNull RuleActionType getType() {
        return RuleActionType.EXECUTE;
    }

    //TODO javadocs
    @Override
    public void run(@NotNull final LivingEntity livingEntity) {
        //TODO
    }

    public interface Executable {

        //TODO javadocs
        void run(@NotNull final LivingEntity livingEntity, @NotNull final String[] args);

    }
}
