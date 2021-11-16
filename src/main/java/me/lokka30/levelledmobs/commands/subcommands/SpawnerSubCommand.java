/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.PaperUtils;
import me.lokka30.levelledmobs.misc.SpigotUtils;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Gives the user a specialized spawner that only spawns mobs within certain level
 * criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
public class SpawnerSubCommand extends SpawnerBaseClass implements Subcommand{

    public SpawnerSubCommand(final LevelledMobs main) {
        super(main);
        startingArgNum = 2;
    }

    final private List<String> allSpawnerOptions = Arrays.asList(
            "/name", "/customdropid", "/spawntype", "/giveplayer", "/lore", "/minlevel", "/maxlevel", "/delay",
            "/maxnearbyentities", "/minspawndelay", "/maxspawndelay", "/requiredplayerrange",
            "/spawncount", "/spawnrange", "/nolore"
    );

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 2){
            showMessage("command.levelledmobs.spawner.usage");
            return;
        }

        OperationEnum operationEnum = OperationEnum.CREATE;

        switch (args[1].toLowerCase()){
            case "copy":
                operationEnum = OperationEnum.COPY;
                break;
            case "info":
                operationEnum = OperationEnum.INFO;
                break;
        }

        boolean hasGivePlayer = false;
        for (int i = 2; i < args.length; i++){
            if ("/giveplayer".equalsIgnoreCase(args[i])){
                hasGivePlayer = true;
                break;
            }
        }

        if ((!hasGivePlayer || operationEnum != OperationEnum.CREATE) && !(sender instanceof Player)){
            final String messageName = operationEnum != OperationEnum.CREATE ?
                "common.no-player" : "command.levelledmobs.spawner.no-player";

            showMessage(messageName);
            return;
        }

        switch (args[1].toLowerCase()){
            case "create":
                parseCreateCommand(sender, label, args);
                break;
            case "copy":
                parseCopyCommand(sender, label, args);
                break;
            case "info":
                parseInfoCommand(sender, label, args);
                break;
        }
    }

    private void parseInfoCommand(@NotNull final CommandSender sender, final String label, final String @NotNull [] args){
        final UUID playerId = ((Player) sender).getUniqueId();

        if (args.length == 2){
            showMessage(main.companion.spawner_InfoIds.contains(playerId) ?
                    "command.levelledmobs.spawner.info.status-enabled" : "command.levelledmobs.spawner.info.status-not-enabled");
            return;
        }

        if ("on".equalsIgnoreCase(args[2])){
            if (main.companion.spawner_CopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(sender, playerId, label);
            }

            main.companion.spawner_InfoIds.add(playerId);
            showMessage("command.levelledmobs.spawner.info.enabled");
        }
        else if ("off".equalsIgnoreCase(args[2]))
            infoGotDisabled(sender, playerId, label);
    }

    private void parseCopyCommand(@NotNull final CommandSender sender, final String label, final String[] args){
        if (!sender.hasPermission("levelledmobs.command.spawner.copy")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        final UUID playerId = ((Player) sender).getUniqueId();

        if (args.length == 2){
            showMessage(main.companion.spawner_CopyIds.contains(playerId) ?
                    "command.levelledmobs.spawner.copy.status-enabled" : "command.levelledmobs.spawner.copy.status-not-enabled");
            return;
        }

        if ("on".equalsIgnoreCase(args[2])){
            if (main.companion.spawner_InfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(sender, playerId, label);
            }

            main.companion.spawner_CopyIds.add(playerId);
            showMessage("command.levelledmobs.spawner.copy.enabled");
        }
        else if ("off".equalsIgnoreCase(args[2]))
            copyGotDisabled(sender, playerId, label);
    }

    private void copyGotDisabled(final @NotNull CommandSender sender, final UUID playerId, final String label){
        main.companion.spawner_CopyIds.remove(playerId);
        showMessage("command.levelledmobs.spawner.copy.disabled");
    }

    private void infoGotDisabled(final @NotNull CommandSender sender, final UUID playerId, final String label){
        main.companion.spawner_InfoIds.remove(playerId);
        showMessage("command.levelledmobs.spawner.info.disabled");
    }

    private void parseCreateCommand(@NotNull final CommandSender sender, final String label, final String[] args){
        hadInvalidArg = false;

        final CustomSpawnerInfo info = new CustomSpawnerInfo(main, label);
        if (sender instanceof Player)
            info.player = (Player) sender;

        // arguments with no values go here:
        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];
            if ("/nolore".equalsIgnoreCase(arg)) {
                info.noLore = true;
                break;
            }
        }

        for (int i = 0; i < allSpawnerOptions.size() - 1; i++){
            final boolean mustBeANumber = (i > 4);
            final String command = allSpawnerOptions.get(i);
            final String foundValue = getArgValue(command, args, mustBeANumber);
            if (hadInvalidArg) return;
            if (Utils.isNullOrEmpty(foundValue)) continue;

            switch (command){
                case "/name": info.customName = foundValue; break;
                case "/customdropid": info.customDropId = foundValue; break;
                case "/lore": info.customLore = foundValue; break;
                case "/spawntype":
                    try{
                        info.spawnType = EntityType.valueOf(foundValue.toUpperCase());
                    }
                    catch (Exception ignored){
                        sender.sendMessage("Invalid spawn type: " + foundValue);
                        return;
                    }
                    break;
                case "/minlevel": info.minLevel = Integer.parseInt(foundValue); break;
                case "/maxlevel": info.maxLevel = Integer.parseInt(foundValue); break;
                case "/delay": info.delay = Integer.parseInt(foundValue); break;
                case "/maxnearbyentities": info.maxNearbyEntities = Integer.parseInt(foundValue); break;
                case "/minspawndelay": info.minSpawnDelay = Integer.parseInt(foundValue); break;
                case "/maxspawndelay": info.maxSpawnDelay = Integer.parseInt(foundValue); break;
                case "/requiredplayerrange": info.requiredPlayerRange = Integer.parseInt(foundValue); break;
                case "/spawncount": info.spawnCount = Integer.parseInt(foundValue); break;
                case "/spawnrange": info.spawnRange = Integer.parseInt(foundValue); break;
                case "/giveplayer":
                    if (Utils.isNullOrEmpty(foundValue)){
                        showMessage("command.levelledmobs.spawner.no-player-specified");
                        return;
                    }
                    try { info.player = Bukkit.getPlayer(foundValue); }
                    catch (Exception e){
                        showMessage("common.player-offline", "%player%", foundValue);
                        return;
                    }
                    if (info.player == null){
                        showMessage("common.player-offline", "%player%", foundValue);
                        return;
                    }
                    break;
            }
        }

        if (info.minLevel == -1 && info.maxLevel == -1) {
            showMessage("command.levelledmobs.spawner.no-level-specified");
            return;
        }

        if (info.player == null){
            showMessage("command.levelledmobs.spawner.no-player-specified");
            return;
        }

        generateSpawner(info);
    }

    public void generateSpawner(final @NotNull CustomSpawnerInfo info){
        if (info.maxSpawnDelay != null && (info.minSpawnDelay == null || info.minSpawnDelay > info.maxSpawnDelay)) {
            // settting max spawn delay lower than min spawn delay will result in an exception
            info.minSpawnDelay = info.maxSpawnDelay;
        }

        if (info.minSpawnDelay != null && (info.maxSpawnDelay == null || info.maxSpawnDelay > info.minSpawnDelay)) {
            // settting min spawn delay higher than max spawn delay will result in an exception
            info.maxSpawnDelay = info.minSpawnDelay;
        }

        if (info.customName != null) info.customName = MessageUtils.colorizeAll(info.customName);

        final ItemStack item = new ItemStack(Material.SPAWNER);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null){
            setMetaItems(meta, info);

            meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner, PersistentDataType.INTEGER, 1);
            meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_MinLevel, PersistentDataType.INTEGER, info.minLevel);
            meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_MaxLevel, PersistentDataType.INTEGER, info.maxLevel);
            if (!Utils.isNullOrEmpty(info.customDropId))
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_CustomDropId, PersistentDataType.STRING, info.customDropId);
            if (!info.spawnType.equals(EntityType.UNKNOWN))
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_SpawnType, PersistentDataType.STRING, info.spawnType.toString());
            if (info.spawnRange != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_SpawnRange, PersistentDataType.INTEGER, info.spawnRange);
            if (info.minSpawnDelay != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_MinSpawnDelay, PersistentDataType.INTEGER, info.minSpawnDelay);
            if (info.maxSpawnDelay != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_MaxSpawnDelay, PersistentDataType.INTEGER, info.maxSpawnDelay);
            if (info.spawnCount != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_SpawnCount, PersistentDataType.INTEGER, info.spawnCount);
            if (info.requiredPlayerRange != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_RequiredPlayerRange, PersistentDataType.INTEGER, info.requiredPlayerRange);
            if (info.maxNearbyEntities != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_MaxNearbyEntities, PersistentDataType.INTEGER, info.maxNearbyEntities);
            if (info.delay != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_Delay, PersistentDataType.INTEGER, info.delay);
            if (info.customName != null)
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_CustomName, PersistentDataType.STRING, info.customName);

            item.setItemMeta(meta);
        }

        int useInvSlotNum = info.player.getInventory().getHeldItemSlot();
        if (info.player.getInventory().getItem(useInvSlotNum) != null)
            useInvSlotNum = -1;

        if (useInvSlotNum == -1) {
            for (int i = 0; i <= 35; i++) {
                if (info.player.getInventory().getItem(i) == null) {
                    useInvSlotNum = i;
                    break;
                }
            }
        }

        if (useInvSlotNum == -1){
            showMessage("command.levelledmobs.spawner.inventory-full", info.player);
            return;
        }

        info.player.getInventory().setItem(useInvSlotNum, item);
        final String playerName = VersionUtils.isRunningPaper() ?
                PaperUtils.getPlayerDisplayName(info.player) : SpigotUtils.getPlayerDisplayName(info.player);

        final List<String> message = getMessage("command.levelledmobs.spawner.spawner-give-message-console",
                new String[]{ "%minlevel%", "%maxlevel%", "%playername%" },
                new String[]{ info.minLevel + "", info.maxLevel + "", playerName }
        );

        if (!message.isEmpty()) Utils.logger.info(message.get(0).replace(main.configUtils.getPrefix() + " ", ""));

        showMessage("command.levelledmobs.spawner.spawner-give-message", info.player);
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final @NotNull CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.spawner"))
            return Collections.emptyList();

        if (args.length <= 2)
            return Arrays.asList("copy", "create", "info");

        if ("create".equalsIgnoreCase(args[1]))
            return tabCompletions_Create(args);
        else if (("info".equalsIgnoreCase(args[1]) || "copy".equalsIgnoreCase(args[1])) && args.length <= 3)
            return Arrays.asList("on", "off");

        return Collections.emptyList();
    }

    private @NotNull List<String> tabCompletions_Create(@NotNull final String @NotNull [] args){
        if (!Utils.isNullOrEmpty(args[args.length - 2])) {
            switch (args[args.length - 2].toLowerCase()){
                case "/spawntype":
                    final List<String> entityNames = new LinkedList<>();
                    for (EntityType entityType : EntityType.values())
                        entityNames.add(entityType.toString().toLowerCase());

                    return entityNames;
                case "/delay":
                    return Collections.singletonList("0");
                case "/minspawndelay":
                    return Collections.singletonList("200");
                case "/maxspawndelay":
                    return Collections.singletonList("800");
                case "/maxnearbyentities":
                case "/requiredplayerrange":
                    return Collections.singletonList("16");
                case "/spawncount":
                case "/spawnrange":
                    return Collections.singletonList("4");
                case "/giveplayer":
                    final List<String> players = new LinkedList<>();
                    for (final Player player : Bukkit.getOnlinePlayers())
                        players.add(player.getName());
                    players.sort(String.CASE_INSENSITIVE_ORDER);
                    return players;
            }
        }

        return checkTabCompletion(allSpawnerOptions, args);
    }

    private enum OperationEnum{
        CREATE,
        COPY,
        INFO
    }
}
