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
            }
            case "exit-all" -> {
                event.getProcess().getActions().add(new ExitAllAction(
                    event.getProcess(), event.getNode()
                ));
            }
            case "exit-function" -> {
                event.getProcess().getActions().add(new ExitFunctionAction(
                    event.getProcess(), event.getNode()
                ));
            }
            case "exit-process" -> {
                event.getProcess().getActions().add(new ExitProcessAction(
                    event.getProcess(), event.getNode()
                ));
            }
            default -> {
                // action does not belong to LM
                return;
            }
        }
        event.setClaimed(true);
    }
}
