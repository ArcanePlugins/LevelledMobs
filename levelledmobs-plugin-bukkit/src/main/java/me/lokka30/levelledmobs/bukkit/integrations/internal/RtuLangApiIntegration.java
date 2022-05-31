package me.lokka30.levelledmobs.bukkit.integrations.internal;

import me.lokka30.levelledmobs.bukkit.integrations.Integration;
import me.lokka30.levelledmobs.bukkit.integrations.type.TranslationProvider;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RtuLangApiIntegration extends Integration implements TranslationProvider {

    public RtuLangApiIntegration() {
        super(
            "Allows usage of RTULangAPI for automatic translations",
            true,
            true
        );
    }

    @Override
    public @Nullable String getTranslatedEntityName(
        @NotNull EntityType entityType,
        @NotNull String locale
    ) {
        //TODO implement this.
        return null;
    }
}
