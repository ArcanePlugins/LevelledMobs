package me.lokka30.levelledmobs.bukkit.integrations.internal;

import me.lokka30.levelledmobs.bukkit.integrations.Integration;
import me.lokka30.levelledmobs.bukkit.integrations.type.MobOwner;
import me.lokka30.levelledmobs.bukkit.utils.TriState;
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
