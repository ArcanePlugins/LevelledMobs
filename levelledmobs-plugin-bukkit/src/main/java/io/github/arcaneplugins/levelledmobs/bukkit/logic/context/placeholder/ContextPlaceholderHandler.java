package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.impl.StandardPlaceholders;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.StringUtils;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
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

    public @Nonnull String replace(@Nonnull String input, final @Nonnull Context context) {
        for(final ContextPlaceholder placeholder : getContextPlaceholders()) {
            input = placeholder.replace(input, context);
        }

        for(final Entry<String, Supplier<Object>> entry : context
            .getMiscContextMap().entrySet()
        ) {
            final String placeholder = entry.getKey();
            final Supplier<Object> supplier = entry.getValue();
            input = StringUtils.replaceIfExists(input, placeholder, supplier);
        }

        return input;
    }

    @NotNull
    public Set<ContextPlaceholder> getContextPlaceholders() {
        return contextPlaceholders;
    }

}
