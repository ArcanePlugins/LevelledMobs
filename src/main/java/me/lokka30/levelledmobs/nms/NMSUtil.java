package me.lokka30.levelledmobs.nms;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A common interface for sending nametag packets
 *
 * @author stumper66
 * @since 3.6.0
 */
public interface NMSUtil {

    void sendNametag(final @NotNull LivingEntity livingEntity, @Nullable String nametag,
        @NotNull Player player, final boolean doAlwaysVisible);
}
