package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.jetbrains.annotations.NotNull;

public interface ContextPlaceholder {

    @NotNull
    String replace(final String from, final Context context);

}
