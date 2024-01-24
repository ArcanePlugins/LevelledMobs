package io.github.arcaneplugins.levelledmobs.misc

import org.bukkit.entity.Player

/**
 * Holds info on a player when they join the server or world
 *
 * @author stumper66
 * @since 3.2.3
 */
class PlayerQueueItem(
    val player: Player,
    val isPlayerJoin: Boolean
)