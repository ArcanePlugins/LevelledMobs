package me.lokka30.levelledmobs.misc;

import org.bukkit.entity.Player;

public class PlayerQueueItem {
    public PlayerQueueItem(final Player player, final boolean isPlayerJoin){
        this.player = player;
        this.isPlayerJoin = isPlayerJoin;
    }

    public final Player player;
    public final boolean isPlayerJoin;
}
