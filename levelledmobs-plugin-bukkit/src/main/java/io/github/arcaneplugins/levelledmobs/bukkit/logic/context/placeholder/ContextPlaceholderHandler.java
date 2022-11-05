package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.HashSet;
import java.util.Set;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl.StandardPlaceholders;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ContextPlaceholderHandler {

    private final Set<ContextPlaceholder> contextPlaceholders = new HashSet<>();

    public void load() {
        Log.inf("Loading context placeholders");

        getContextPlaceholders().clear();

        getContextPlaceholders().add(new StandardPlaceholders());

        Bukkit.getPluginManager().callEvent(new ContextPlaceholdersLoadEvent());
    }

    public String replace(String input, final Context context) {
        if(input == null || input.isBlank()) {
            return "";
        }

        for(var placeholder : getContextPlaceholders()) {
            input = placeholder.replace(input, context);
        }

        return input;
    }

    @NotNull
    public Set<ContextPlaceholder> getContextPlaceholders() {
        return contextPlaceholders;
    }

}
