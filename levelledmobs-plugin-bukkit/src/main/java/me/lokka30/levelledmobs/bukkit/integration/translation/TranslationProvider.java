package me.lokka30.levelledmobs.bukkit.integration.translation;

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
