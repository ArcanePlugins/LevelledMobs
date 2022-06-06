package me.lokka30.levelledmobs.bukkit.integration.internal;

import me.lokka30.levelledmobs.bukkit.integration.Integration;
import me.lokka30.levelledmobs.bukkit.integration.MobOwner;
import me.lokka30.levelledmobs.bukkit.util.TriState;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class CitizensIntegration extends Integration implements MobOwner {

    public CitizensIntegration() {
        super(
            "Detects mobs which are Citizens NPCs",
            true,
            true
        );
    }

    @Override
    public @NotNull TriState ownsMob(@NotNull LivingEntity entity) {
        // TODO test this.
        return TriState.of(entity.hasMetadata("NPC"));
    }
}
