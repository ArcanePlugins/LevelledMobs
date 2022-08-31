package me.lokka30.levelledmobs.bukkit.logic.context.placeholder;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.EntityLevelPlaceholder;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.EntityNamePlaceholder;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal.EntityTypePlaceholder;
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
            new EntityNamePlaceholder(),
            new EntityTypePlaceholder(),
            new PlayerNamePlaceholder()
        ));

        Bukkit.getPluginManager().callEvent(new ContextPlaceholdersLoadEvent());
    }

    public String replace(final String from, final Context context) {
        String current = from;
        for(var placeholder : getContextPlaceholders()) {
            current = placeholder.replace(from, context);
        }
        return current;
    }

    @NotNull
    public Set<ContextPlaceholder> getContextPlaceholders() {
        return contextPlaceholders;
    }

}
