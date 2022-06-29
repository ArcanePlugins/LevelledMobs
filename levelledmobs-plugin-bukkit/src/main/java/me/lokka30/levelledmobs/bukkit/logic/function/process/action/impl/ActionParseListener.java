package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import java.util.Locale;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.ActionParseEvent;
import me.lokka30.levelledmobs.bukkit.listener.ListenerWrapper;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
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
        final var process = event.getProcess();
        final var node = event.getNode();

        switch (event.getIdentifier().toLowerCase(Locale.ROOT)) {
            case "__test" -> {
                addAction(event, new TestAction(process, node));
            }
            case "add-nbt-tag" -> {
                addAction(event, new AddNbtTagAction(process, node));
            }
            case "broadcast-message-to-nearby-players" -> {
                addAction(event, new BroadcastMessageToNearbyPlayersAction(process, node));
            }
            case "broadcast-message-to-server" -> {
                addAction(event, new BroadcastMessageToServerAction(process, node));
            }
            case "broadcast-message-to-world" -> {
                addAction(event, new BroadcastMessageToWorldAction(process, node));
            }
            case "exit-all" -> {
                addAction(event, new ExitAllAction(process, node));
            }
            case "exit-function" -> {
                addAction(event, new ExitFunctionAction(process, node));
            }
            case "exit-process" -> {
                addAction(event, new ExitProcessAction(process, node));
            }
            case "run-function" -> {
                addAction(event, new RunFunctionAction(process, node));
            }
            case "set-drop-table-id" -> {
                addAction(event, new SetDropTableIdAction(process, node));
            }
            case "set-level" -> {
                addAction(event, new SetLevelAction(process, node));
            }
        }
    }

    private void addAction(final ActionParseEvent event, final Action action) {
        event.getProcess().getActions().add(action);
        event.setClaimed(true);
    }
}
