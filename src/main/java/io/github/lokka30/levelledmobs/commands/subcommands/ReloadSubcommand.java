package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

import java.util.List;

public class ReloadSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (sender.hasPermission("levelledmobs.command.reload")) {
            List<String> reloadStartedMsg = instance.messagesCfg.getStringList("command.levelledmobs.reload.started");
            reloadStartedMsg = Utils.replaceAllInList(reloadStartedMsg, "%prefix%", instance.configUtils.getPrefix());
            reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg);
            reloadStartedMsg.forEach(sender::sendMessage);

            instance.loadFiles();

            List<String> reloadFinishedMsg = instance.messagesCfg.getStringList("command.levelledmobs.reload.finished");
            reloadFinishedMsg = Utils.replaceAllInList(reloadFinishedMsg, "%prefix%", instance.configUtils.getPrefix());
            reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg);

            if (instance.settingsCfg.getBoolean("debug-entity-damage") && !instance.debugEntityDamageWasEnabled) {
                instance.debugEntityDamageWasEnabled = true;
                instance.pluginManager.registerEvents(instance.entityDamageDebugListener, instance);
            } else if (!instance.settingsCfg.getBoolean("debug-entity-damage") && instance.debugEntityDamageWasEnabled) {
                instance.debugEntityDamageWasEnabled = false;
                HandlerList.unregisterAll(instance.entityDamageDebugListener);
            }

            reloadFinishedMsg.forEach(sender::sendMessage);
        } else {
            instance.configUtils.sendNoPermissionMsg(sender);
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {
        return null; //No tab completions.
    }
}
