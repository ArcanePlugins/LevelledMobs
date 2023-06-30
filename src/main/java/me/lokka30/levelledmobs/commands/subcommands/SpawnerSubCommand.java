/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.PaperUtils;
import me.lokka30.levelledmobs.util.SpigotUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Gives the user a specialized spawner that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
public class SpawnerSubCommand extends SpawnerBaseClass implements Subcommand {

    public SpawnerSubCommand(final LevelledMobs main) {
        super(main);
        startingArgNum = 2;
    }

    final private List<String> allSpawnerOptions = Arrays.asList(
        "/name", "/customdropid", "/spawntype", "/giveplayer", "/lore", "/minlevel", "/maxlevel",
        "/delay",
        "/maxnearbyentities", "/minspawndelay", "/maxspawndelay", "/requiredplayerrange",
        "/spawncount", "/spawnrange", "/nolore"
    );

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender,
        final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 2) {
            showMessage("command.levelledmobs.spawner.usage");
            return;
        }

        OperationEnum operationEnum = switch (args[1].toLowerCase()) {
            case "copy" -> OperationEnum.COPY;
            case "info" -> OperationEnum.INFO;
            default -> OperationEnum.CREATE;
        };

        boolean hasGivePlayer = false;
        for (int i = 2; i < args.length; i++) {
            if ("/giveplayer".equalsIgnoreCase(args[i])) {
                hasGivePlayer = true;
                break;
            }
        }

        if ((!hasGivePlayer || operationEnum != OperationEnum.CREATE)
            && !(sender instanceof Player)) {
            final String messageName = operationEnum != OperationEnum.CREATE ?
                "common.players-only" : "command.levelledmobs.spawner.no-player";

            showMessage(messageName);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> parseCreateCommand(args);
            case "copy" -> parseCopyCommand(args);
            case "info" -> parseInfoCommand(args);
        }
    }

    private void parseInfoCommand(final String @NotNull [] args) {
        final UUID playerId = ((Player) commandSender).getUniqueId();

        if (args.length == 2) {
            showMessage(main.companion.spawnerInfoIds.contains(playerId) ?
                "command.levelledmobs.spawner.info.status-enabled"
                : "command.levelledmobs.spawner.info.status-not-enabled");
            return;
        }

        if ("on".equalsIgnoreCase(args[2])) {
            if (main.companion.spawnerCopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(playerId);
            }

            main.companion.spawnerInfoIds.add(playerId);
            showMessage("command.levelledmobs.spawner.info.enabled");
        } else if ("off".equalsIgnoreCase(args[2])) {
            infoGotDisabled(playerId);
        }
    }

    private void parseCopyCommand(final String[] args) {
        if (!commandSender.hasPermission("levelledmobs.command.spawner.copy")) {
            main.configUtils.sendNoPermissionMsg(commandSender);
            return;
        }

        final UUID playerId = ((Player) commandSender).getUniqueId();

        if (args.length == 2) {
            showMessage(main.companion.spawnerCopyIds.contains(playerId) ?
                "command.levelledmobs.spawner.copy.status-enabled"
                : "command.levelledmobs.spawner.copy.status-not-enabled");
            return;
        }

        if ("on".equalsIgnoreCase(args[2])) {
            if (main.companion.spawnerInfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(playerId);
            }

            main.companion.spawnerCopyIds.add(playerId);
            showMessage("command.levelledmobs.spawner.copy.enabled");
        } else if ("off".equalsIgnoreCase(args[2])) {
            copyGotDisabled(playerId);
        }
    }

    private void copyGotDisabled(final UUID playerId) {
        main.companion.spawnerCopyIds.remove(playerId);
        showMessage("command.levelledmobs.spawner.copy.disabled");
    }

    private void infoGotDisabled(final UUID playerId) {
        main.companion.spawnerInfoIds.remove(playerId);
        showMessage("command.levelledmobs.spawner.info.disabled");
    }

    private void parseCreateCommand(final String[] args) {
        hadInvalidArg = false;

        final CustomSpawnerInfo info = new CustomSpawnerInfo(main, messageLabel);
        if (commandSender instanceof Player) {
            info.player = (Player) commandSender;
        }

        // arguments with no values go here:
        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];
            if ("/nolore".equalsIgnoreCase(arg)) {
                info.noLore = true;
                break;
            }
        }

        for (int i = 0; i < allSpawnerOptions.size() - 1; i++) {
            final boolean mustBeANumber = (i > 4);
            final String command = allSpawnerOptions.get(i);
            final String foundValue = getArgValue(command, args, mustBeANumber);
            if (hadInvalidArg) {
                return;
            }
            if (Utils.isNullOrEmpty(foundValue)) {
                continue;
            }

            switch (command) {
                case "/name" -> info.customName = foundValue;
                case "/customdropid" -> info.customDropId = foundValue;
                case "/lore" -> info.customLore = foundValue;
                case "/spawntype" -> {
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.toUpperCase());
                    } catch (final Exception ignored) {
                        commandSender.sendMessage("Invalid spawn type: " + foundValue);
                        return;
                    }
                }
                case "/minlevel" -> info.minLevel = Integer.parseInt(foundValue);
                case "/maxlevel" -> info.maxLevel = Integer.parseInt(foundValue);
                case "/delay" -> info.delay = Integer.parseInt(foundValue);
                case "/maxnearbyentities" -> info.maxNearbyEntities = Integer.parseInt(foundValue);
                case "/minspawndelay" -> info.minSpawnDelay = Integer.parseInt(foundValue);
                case "/maxspawndelay" -> info.maxSpawnDelay = Integer.parseInt(foundValue);
                case "/requiredplayerrange" -> info.requiredPlayerRange = Integer.parseInt(foundValue);
                case "/spawncount" -> info.spawnCount = Integer.parseInt(foundValue);
                case "/spawnrange" -> info.spawnRange = Integer.parseInt(foundValue);
                case "/giveplayer" -> {
                    if (Utils.isNullOrEmpty(foundValue)) {
                        showMessage("command.levelledmobs.spawner.no-player-specified");
                        return;
                    }
                    try {
                        info.player = Bukkit.getPlayer(foundValue);
                    } catch (final Exception e) {
                        showMessage("common.player-offline", "%player%", foundValue);
                        return;
                    }
                    if (info.player == null) {
                        showMessage("common.player-offline", "%player%", foundValue);
                        return;
                    }
                }
            }
        }

        if (info.minLevel == -1 && info.maxLevel == -1) {
            showMessage("command.levelledmobs.spawner.no-level-specified");
            return;
        }

        if (info.player == null) {
            showMessage("command.levelledmobs.spawner.no-player-specified");
            return;
        }

        generateSpawner(info);
    }

    public void generateSpawner(final @NotNull CustomSpawnerInfo info) {
        if (info.customName != null) {
            info.customName = MessageUtils.colorizeAll(info.customName);
        }

        final ItemStack item = new ItemStack(Material.SPAWNER);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setMetaItems(meta, info, "LM Spawner");

            meta.getPersistentDataContainer()
                .set(info.main.namespacedKeys.keySpawner, PersistentDataType.INTEGER, 1);
            meta.getPersistentDataContainer()
                .set(info.main.namespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER,
                    info.minLevel);
            meta.getPersistentDataContainer()
                .set(info.main.namespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER,
                    info.maxLevel);
            if (!Utils.isNullOrEmpty(info.customDropId)) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerCustomDropId,
                        PersistentDataType.STRING, info.customDropId);
            }
            if (info.spawnType != EntityType.UNKNOWN) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING,
                        info.spawnType.toString());
            }
            if (info.spawnRange != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerSpawnRange,
                        PersistentDataType.INTEGER, info.spawnRange);
            }
            if (info.minSpawnDelay != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerMinSpawnDelay,
                        PersistentDataType.INTEGER, info.minSpawnDelay);
            }
            if (info.maxSpawnDelay != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerMaxSpawnDelay,
                        PersistentDataType.INTEGER, info.maxSpawnDelay);
            }
            if (info.spawnCount != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerSpawnCount,
                        PersistentDataType.INTEGER, info.spawnCount);
            }
            if (info.requiredPlayerRange != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerRequiredPlayerRange,
                        PersistentDataType.INTEGER, info.requiredPlayerRange);
            }
            if (info.maxNearbyEntities != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerMaxNearbyEntities,
                        PersistentDataType.INTEGER, info.maxNearbyEntities);
            }
            if (info.delay != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerDelay, PersistentDataType.INTEGER,
                        info.delay);
            }
            if (info.customName != null) {
                meta.getPersistentDataContainer()
                    .set(info.main.namespacedKeys.keySpawnerCustomName, PersistentDataType.STRING,
                        info.customName);
            }

            item.setItemMeta(meta);
        }

        int useInvSlotNum = info.player.getInventory().getHeldItemSlot();
        if (info.player.getInventory().getItem(useInvSlotNum) != null) {
            useInvSlotNum = -1;
        }

        if (useInvSlotNum == -1) {
            for (int i = 0; i <= 35; i++) {
                if (info.player.getInventory().getItem(i) == null) {
                    useInvSlotNum = i;
                    break;
                }
            }
        }

        if (useInvSlotNum == -1) {
            showMessage("command.levelledmobs.spawner.inventory-full", info.player);
            return;
        }

        info.player.getInventory().setItem(useInvSlotNum, item);
        final String playerName = main.getVerInfo().getIsRunningPaper() ?
            PaperUtils.getPlayerDisplayName(info.player)
            : SpigotUtils.getPlayerDisplayName(info.player);

        final List<String> message = getMessage(
            "command.levelledmobs.spawner.spawner-give-message-console",
            new String[]{"%minlevel%", "%maxlevel%", "%playername%"},
            new String[]{String.valueOf(info.minLevel), String.valueOf(info.maxLevel), playerName}
        );

        if (!message.isEmpty()) {
            Utils.logger.info(message.get(0).replace(main.configUtils.getPrefix() + " ", ""));
        }

        showMessage("command.levelledmobs.spawner.spawner-give-message", info.player);
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main,
        final @NotNull CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            return Collections.emptyList();
        }

        if (args.length <= 2) {
            return Arrays.asList("copy", "create", "info");
        }

        if ("create".equalsIgnoreCase(args[1])) {
            return tabCompletions_Create(args);
        } else if (("info".equalsIgnoreCase(args[1]) || "copy".equalsIgnoreCase(args[1]))
            && args.length == 3) {
            return Arrays.asList("on", "off");
        }

        return Collections.emptyList();
    }

    @NotNull private List<String> tabCompletions_Create(@NotNull final String @NotNull [] args) {
        if (!Utils.isNullOrEmpty(args[args.length - 2])) {
            switch (args[args.length - 2].toLowerCase()) {
                case "/spawntype" -> {
                    final List<String> entityNames = new LinkedList<>();
                    for (final EntityType entityType : EntityType.values()) {
                        entityNames.add(entityType.toString().toLowerCase());
                    }
                    return entityNames;
                }
                case "/delay" -> {
                    return Collections.singletonList("0");
                }
                case "/minspawndelay" -> {
                    return Collections.singletonList("200");
                }
                case "/maxspawndelay" -> {
                    return Collections.singletonList("800");
                }
                case "/maxnearbyentities", "/requiredplayerrange" -> {
                    return Collections.singletonList("16");
                }
                case "/spawncount", "/spawnrange" -> {
                    return Collections.singletonList("4");
                }
                case "/giveplayer" -> {
                    final List<String> players = new LinkedList<>();
                    for (final Player player : Bukkit.getOnlinePlayers()) {
                        players.add(player.getName());
                    }
                    players.sort(String.CASE_INSENSITIVE_ORDER);
                    return players;
                }
            }
        }

        return checkTabCompletion(allSpawnerOptions, args);
    }

    private enum OperationEnum {
        CREATE,
        COPY,
        INFO
    }
}
