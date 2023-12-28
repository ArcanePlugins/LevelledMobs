package me.lokka30.levelledmobs.misc;

import org.bukkit.entity.Player;

/**
 * Holds info on a player when they join the server or world
 *
 * @author stumper66
 * @since 3.2.3
 */
public class PlayerQueueItem {

    public PlayerQueueItem(final Player player, final boolean isPlayerJoin) {
        this.player = player;
        this.isPlayerJoin = isPlayerJoin;
    }

    public final Player player;
    final boolean isPlayerJoin;
}
