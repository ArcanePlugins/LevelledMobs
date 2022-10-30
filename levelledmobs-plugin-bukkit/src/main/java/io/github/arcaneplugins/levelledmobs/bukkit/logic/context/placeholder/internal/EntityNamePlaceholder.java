package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.internal;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.TranslationHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;

/*
%entity-name%: translated entity name e.g. 'Brain Eater'
 */
public class EntityNamePlaceholder implements ContextPlaceholder {

    private final TranslationHandler translationHandler = LevelledMobs.getInstance()
        .getConfigHandler()
        .getTranslationHandler();

    @Override
    public @NotNull String replace(final @NotNull String from, final @NotNull Context context) {
        return from.replace("%entity-name%", "{EntityName}");
    }
}
