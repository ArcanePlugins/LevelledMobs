package me.lokka30.levelledmobs.bukkit.event.action;

import me.lokka30.levelledmobs.bukkit.logic.Action;
import org.jetbrains.annotations.NotNull;

public interface ActionEvent {

    @NotNull
    Action getAction();

}
