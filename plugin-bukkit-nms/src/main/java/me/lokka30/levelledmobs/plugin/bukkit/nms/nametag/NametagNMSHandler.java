package me.lokka30.levelledmobs.plugin.bukkit.nms.nametag;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NametagNMSHandler {

    void sendNametag(
        final @NotNull LivingEntity livingEntity,
        @Nullable String nametag,
        @NotNull Player player,
        final boolean alwaysVisible
    );
}
