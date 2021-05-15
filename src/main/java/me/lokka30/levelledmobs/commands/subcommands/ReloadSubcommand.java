package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class ReloadSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.reload")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        List<String> reloadStartedMsg = main.messagesCfg.getStringList("command.levelledmobs.reload.started");
        reloadStartedMsg = Utils.replaceAllInList(reloadStartedMsg, "%prefix%", main.configUtils.getPrefix());
        reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg);
        reloadStartedMsg.forEach(sender::sendMessage);

        main.companion.loadFiles();

        List<String> reloadFinishedMsg = main.messagesCfg.getStringList("command.levelledmobs.reload.finished");
        reloadFinishedMsg = Utils.replaceAllInList(reloadFinishedMsg, "%prefix%", main.configUtils.getPrefix());
        reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg);

        if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            if (ExternalCompatibilityManager.hasProtocolLibInstalled() && (main.levelManager.nametagAutoUpdateTask == null || main.levelManager.nametagAutoUpdateTask.isCancelled()))
                main.levelManager.startNametagAutoUpdateTask();
            else if (!ExternalCompatibilityManager.hasProtocolLibInstalled() && main.levelManager.nametagAutoUpdateTask != null && !main.levelManager.nametagAutoUpdateTask.isCancelled())
                main.levelManager.stopNametagAutoUpdateTask();
        }

        if (main.settingsCfg.getBoolean("debug-entity-damage") && !main.configUtils.debugEntityDamageWasEnabled) {
            main.configUtils.debugEntityDamageWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(main.entityDamageDebugListener, main);
        } else if (!main.settingsCfg.getBoolean("debug-entity-damage") && main.configUtils.debugEntityDamageWasEnabled) {
            main.configUtils.debugEntityDamageWasEnabled = false;
            HandlerList.unregisterAll(main.entityDamageDebugListener);
        }

        if (main.settingsCfg.getBoolean("ensure-mobs-are-levelled-on-chunk-load") && !main.configUtils.chunkLoadListenerWasEnabled) {
            main.configUtils.chunkLoadListenerWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(main.chunkLoadListener, main);
        } else if (!main.settingsCfg.getBoolean("ensure-mobs-are-levelled-on-chunk-load") && main.configUtils.chunkLoadListenerWasEnabled) {
            main.configUtils.chunkLoadListenerWasEnabled = false;
            HandlerList.unregisterAll(main.chunkLoadListener);
        }

        reloadFinishedMsg.forEach(sender::sendMessage);
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args) {
        return null; //No tab completions.
    }
}
