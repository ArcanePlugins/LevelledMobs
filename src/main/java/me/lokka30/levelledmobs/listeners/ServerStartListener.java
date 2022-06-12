package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.FileLoader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerStartListener implements Listener {

    public ServerStartListener(final LevelledMobs main) {
        this.main = main;
    }

    private final LevelledMobs main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onServerLoadEvent(final ServerLoadEvent event) {
        main.customDropsHandler.customDropsParser.loadDrops(
            FileLoader.loadFile(main, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
        );
    }
}
