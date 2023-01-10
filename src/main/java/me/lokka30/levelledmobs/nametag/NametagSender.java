package me.lokka30.levelledmobs.nametag;

import me.lokka30.levelledmobs.result.NametagResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A common interface for sending nametag packets
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
public interface NametagSender {

    void sendNametag(
        final @NotNull LivingEntity livingEntity,
        final @NotNull NametagResult nametag,
        final @NotNull Player player,
        final boolean alwaysVisible
    );
}
