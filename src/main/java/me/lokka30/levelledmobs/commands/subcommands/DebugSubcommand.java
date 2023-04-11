package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.DebugCreator;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.nametag.MiscUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Parses commands for various debug stuff
 *
 * @author stumper66
 * @since 3.2.0
 */

public class DebugSubcommand extends MessagesBase implements Subcommand {

    public DebugSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender,
        final String label, final String @NotNull [] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.debug")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length <= 1) {
            sender.sendMessage("Options: create / chunk_kill_count / nbt_dump / mylocation, spawn_distance");
            return;
        }

        if ("create".equalsIgnoreCase(args[1])) {
            if (args.length >= 3 && "confirm".equalsIgnoreCase(args[2])) {
                DebugCreator.createDebug(main, sender);
            } else {
                showMessage("other.create-debug");
            }
        } else if ("chunk_kill_count".equalsIgnoreCase(args[1])) {
            chunkKillCount(sender, args);
        } else if ("nbt_dump".equalsIgnoreCase(args[1])) {
            if (!main.nametagQueueManager.nametagSenderHandler.versionInfo.isNMSVersionValid()){
                sender.sendMessage("Unable to dump, an unknown NMS version was detected");
                return;
            }
            doNbtDump(sender, args);
            if (!(commandSender instanceof ConsoleCommandSender)) {
                sender.sendMessage("NBT data has been written to the console");
            }
        } else if ("mylocation".equalsIgnoreCase(args[1])){
            showPlayerLocation(sender);
        } else if ("spawn_distance".equalsIgnoreCase(args[1])) {
            showSpawnDistance(sender, args);
        } else {
            showMessage("other.create-debug");
        }
    }

    private void showSpawnDistance(final @NotNull CommandSender sender, final String @NotNull [] args){
        Player player = null;
        if (!(sender instanceof Player) && args.length < 3) {
            sender.sendMessage("Must specify a player when running this command from console");
            return;
        }
        if (args.length >= 3) {
            player = Bukkit.getPlayer(args[2]);
            if (player == null) {
                sender.sendMessage("Invalid playername: " + args[2]);
                return;
            }
        }

        if (player == null) {
            player = (Player) sender;
        }

        final LivingEntityWrapper lmEntity = main.levelledMobsCommand.rulesSubcommand.getMobBeingLookedAt(
                player, true, sender);

        if (lmEntity == null){
            sender.sendMessage("Could not locate any mobs near player: " + player.getName());
            return;
        }

        final double distance = lmEntity.getDistanceFromSpawn();

        final String locationStr = String.format("%s, %s, %s",
                lmEntity.getLivingEntity().getLocation().getBlockX(),
                lmEntity.getLivingEntity().getLocation().getBlockY(),
                lmEntity.getLivingEntity().getLocation().getBlockZ());
        final String mobLevel = lmEntity.isLevelled() ? String.valueOf(lmEntity.getMobLevel()) : "0";

        String entityName = lmEntity.getTypeName();
        if (ExternalCompatibilityManager.hasMythicMobsInstalled()
                && ExternalCompatibilityManager.isMythicMob(lmEntity)) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity);
        }

        final String message = String.format(
                "Spawn distance is %s for: %s (lvl %s %s) in %s, %s",
                Utils.round(distance, 1),
                entityName,
                mobLevel,
                lmEntity.getNameIfBaby(),
                lmEntity.getWorldName(),
                locationStr);

        lmEntity.free();
        sender.sendMessage(message);
    }

    private void showPlayerLocation(final @NotNull CommandSender sender){
        if (!(sender instanceof final Player player)){
            sender.sendMessage("The command must be run by a player");
            return;
        }

        final Location l = player.getLocation();
        final String locationStr = String.format("location is %s, %s, %s in %s",
                l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getWorld().getName());
        sender.sendMessage("Your " + locationStr);
        Utils.logger.info(String.format("Player %s %s", player.getName(), locationStr));
    }

    private void doNbtDump(final @NotNull CommandSender sender, final String @NotNull [] args) {
        Player player = null;
        if (!(sender instanceof Player) && args.length < 3) {
            sender.sendMessage("Must specify a player when running this command from console");
            return;
        }
        if (args.length >= 3) {
            player = Bukkit.getPlayer(args[2]);
            if (player == null) {
                sender.sendMessage("Invalid playername: " + args[2]);
                return;
            }
        }

        if (player == null) {
            player = (Player) sender;
        }

        final LivingEntityWrapper lmEntity = main.levelledMobsCommand.rulesSubcommand.getMobBeingLookedAt(
            player, true, sender);
        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + player.getName());
            return;
        }

        String entityName = lmEntity.getTypeName();
        if (ExternalCompatibilityManager.hasMythicMobsInstalled()
            && ExternalCompatibilityManager.isMythicMob(lmEntity)) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity);
        }

        final String locationStr = String.format("%s, %s, %s",
            lmEntity.getLivingEntity().getLocation().getBlockX(),
            lmEntity.getLivingEntity().getLocation().getBlockY(),
            lmEntity.getLivingEntity().getLocation().getBlockZ());
        final String mobLevel = lmEntity.isLevelled() ? String.valueOf(lmEntity.getMobLevel()) : "0";

        final String message = String.format(
            "Showing nbt dump for: %s (lvl %s %s) in %s, %s\n%s",
            entityName,
            mobLevel,
            lmEntity.getNameIfBaby(),
            lmEntity.getWorldName(),
            locationStr,
            MiscUtils.getNBTDump(lmEntity.getLivingEntity())
        );

        lmEntity.free();
        Utils.logger.info(message);
    }

    private void chunkKillCount(final @NotNull CommandSender sender,
        final String @NotNull [] args) {
        if (args.length >= 3 && "reset".equalsIgnoreCase(args[2])) {
            main.companion.clearChunkKillCache();
            sender.sendMessage("cache has been cleared");
            return;
        }

        showChunkKillCountSyntax(sender);
    }

    private void showChunkKillCountSyntax(final @NotNull CommandSender sender) {
        sender.sendMessage("Options: reset");
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender,
        final String @NotNull [] args) {

        if (args.length <= 2) {
            return List.of("create", "chunk_kill_count", "mylocation", "nbt_dump", "spawn_distance");
        }
        if ("chunk_kill_count".equalsIgnoreCase(args[1])) {
            return List.of("reset");
        } else if ("nbt_dump".equalsIgnoreCase(args[1]) && args.length == 3) {
            return null;
        }

        return Collections.emptyList();
    }
}
