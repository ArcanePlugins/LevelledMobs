package me.lokka30.levelledmobs.bukkit.event.condition;

import me.lokka30.levelledmobs.bukkit.logic.Condition;
import org.jetbrains.annotations.NotNull;

public interface ConditionEvent {

    @NotNull
    Condition getCondition();

}
