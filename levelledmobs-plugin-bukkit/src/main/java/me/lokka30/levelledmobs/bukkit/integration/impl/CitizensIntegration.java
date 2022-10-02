package me.lokka30.levelledmobs.bukkit.integration.impl;

import me.lokka30.levelledmobs.bukkit.integration.Integration;
import me.lokka30.levelledmobs.bukkit.integration.IntegrationPriority;
import me.lokka30.levelledmobs.bukkit.integration.type.entity.EntityOwner;
import me.lokka30.levelledmobs.bukkit.util.TriState;
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
