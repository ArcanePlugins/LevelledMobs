package me.lokka30.levelledmobs.bukkit.integration.internal;

import me.lokka30.levelledmobs.bukkit.integration.Integration;
import me.lokka30.levelledmobs.bukkit.integration.IntegrationPriority;
import me.lokka30.levelledmobs.bukkit.integration.translation.TranslationProvider;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RtuLangApiIntegration extends Integration implements TranslationProvider {

    public RtuLangApiIntegration() {
        super(
            "Allows usage of RTULangAPI for automatic translations",
            true,
            true,
            IntegrationPriority.NORMAL
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
