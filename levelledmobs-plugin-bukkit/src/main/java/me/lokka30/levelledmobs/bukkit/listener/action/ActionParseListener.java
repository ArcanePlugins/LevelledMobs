package me.lokka30.levelledmobs.bukkit.listener.action;

import java.util.Locale;
import me.lokka30.levelledmobs.bukkit.event.action.ActionParseEvent;
import me.lokka30.levelledmobs.bukkit.listener.ListenerWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class ActionParseListener extends ListenerWrapper {

    /* constructor */

    public ActionParseListener() {
        super(true);
    }

    /* methods */

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onActionParse(final ActionParseEvent event) {
        switch(event.getIdentifier().toLowerCase(Locale.ROOT)) {
            case "broadcast-message-to-server" -> {
                event.getProcess().getActions().add(new BroadcastMessageToServerAction(
                    event.getProcess(), event.getNode()
                ));
                event.setClaimed(true);
            }
        }
    }
}
