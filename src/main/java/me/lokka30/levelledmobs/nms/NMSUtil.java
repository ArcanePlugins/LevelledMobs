package me.lokka30.levelledmobs.nms;

import me.lokka30.levelledmobs.result.NametagResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A common interface for sending nametag packets
 *
 * @author stumper66
 * @since 3.6.0
 */
public interface NMSUtil {

    void sendNametag(final @NotNull LivingEntity livingEntity, final @NotNull NametagResult nametag,
                     final @NotNull Player player, final boolean doAlwaysVisible);
}
