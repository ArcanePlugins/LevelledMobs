package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;

public class PlayerPortalEventListener implements Listener {
    public PlayerPortalEventListener(final LevelledMobs main){
        this.main = main;
    }

    private final LevelledMobs main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerPortalEvent(final @NotNull PlayerPortalEvent event){
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (event.getTo().getWorld() == null) return;
        final boolean isToNether = (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER);

        final Player player = event.getPlayer();

        // store the player's portal coords in the nether.  only used for player levelling
        main.companion.setPlayerNetherPortalLocation(player, event.getTo());
        final String locationStr = String.format("%s,%s,%s,%s",
                event.getTo().getWorld().getName(), event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());

        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (isToNether)
                    main.companion.setPlayerNetherPortalLocation(player, player.getLocation());
                else
                    main.companion.setPlayerWorldPortalLocation(player, player.getLocation());

                try{
                    if (isToNether)
                        event.getPlayer().getPersistentDataContainer().set(main.namespaced_keys.playerNetherCoords, PersistentDataType.STRING, locationStr);
                    else
                        event.getPlayer().getPersistentDataContainer().set(main.namespaced_keys.playerNetherCoords_IntoWorld, PersistentDataType.STRING, locationStr);
                }
                catch (ConcurrentModificationException e){
                    Utils.logger.warning("Error updating PDC on " + player.getName() + ", " + e.getMessage());
                }
            }
        };

        // for some reason event#getTo has different coords that the actual nether portal
        // delay for 1 ticket and grab the player location instead
        runnable.runTaskLater(main, 1L);
    }
}
