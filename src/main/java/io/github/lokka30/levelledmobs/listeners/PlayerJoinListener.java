package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {

    private final LevelledMobs instance;

    public PlayerJoinListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {

        // Player must have permission
        if (!event.getPlayer().hasPermission("levelledmobs.compatibility-notice")) return;

        // There must be possible incompatibilities
        if (instance.incompatibilitiesAmount == 0) return;

        // Must be enabled in messages cfg
        if (!instance.messagesCfg.getBoolean("other.compatibility-notice.enabled")) return;

        List<String> messages = instance.messagesCfg.getStringList("other.compatibility-notice.messages");
        messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%incompatibilities%", instance.incompatibilitiesAmount + "");
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(event.getPlayer()::sendMessage);
    }
}
