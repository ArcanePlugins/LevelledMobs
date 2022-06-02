package me.lokka30.levelledmobs.bukkit.logic.functions.processes;

import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.functions.processes.actions.Action;
import me.lokka30.levelledmobs.bukkit.logic.functions.processes.conditions.Condition;
import org.jetbrains.annotations.NotNull;

public record Process(
    @NotNull String id,
    @NotNull String description,
    @NotNull Set<String> presets,
    @NotNull Set<Condition> conditions,
    @NotNull Set<Action> actions
) {
    //TODO
}
