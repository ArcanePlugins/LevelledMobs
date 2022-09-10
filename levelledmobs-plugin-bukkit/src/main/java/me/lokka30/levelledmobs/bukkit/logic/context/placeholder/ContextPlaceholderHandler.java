package me.lokka30.levelledmobs.bukkit.logic.context.placeholder;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.EntityLevelPlaceholder;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.EntityPlaceholders;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.PlayerNamePlaceholder;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ContextPlaceholderHandler {

    private final Set<ContextPlaceholder> contextPlaceholders = new HashSet<>();

    public void load() {
        Log.inf("Loading context placeholders");

        getContextPlaceholders().addAll(Set.of(
            //TODO add more of these!
            new EntityLevelPlaceholder(),
            new EntityPlaceholders(),
            new PlayerNamePlaceholder()
        ));

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
