package me.lokka30.levelledmobs.bukkit.logic.context.placeholder;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;

public interface ContextPlaceholder {

    @NotNull
    String replace(final String from, final Context context);

}
