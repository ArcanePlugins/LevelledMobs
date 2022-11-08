package io.github.arcaneplugins.levelledmobs.bukkit.integration.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.entity.EntityOwner;
import io.github.arcaneplugins.levelledmobs.bukkit.api.util.TriState;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.Integration;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.IntegrationPriority;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class CitizensIntegration extends Integration implements EntityOwner {

    public CitizensIntegration() {
        super(
            "Citizens",
            "Detects entities which are Citizens NPCs",
            true,
            true,
            IntegrationPriority.NORMAL
        );
    }

    @Override
    public @NotNull TriState ownsEntity(@NotNull LivingEntity entity) {
        // TODO test this.
        return TriState.of(entity.hasMetadata("NPC"));
    }
}
