package me.lokka30.levelledmobs.bukkit.listener.action;

import me.lokka30.levelledmobs.bukkit.event.process.ProcessPreParseEvent;
import me.lokka30.levelledmobs.bukkit.listener.ListenerWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ActionListener extends ListenerWrapper {

    /* constructor */

    public ActionListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProcessParse(final ProcessPreParseEvent event) {
        
    }
}
