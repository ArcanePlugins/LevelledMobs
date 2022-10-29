package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.translation;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TranslationProvider {

    @Nullable
    String getTranslatedEntityName(
        final @NotNull EntityType entityType,
        final @NotNull String locale
    );

}
