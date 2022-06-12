package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.PaperUtils;
import me.lokka30.levelledmobs.util.SpigotUtils;
import me.lokka30.levelledmobs.util.Utils;
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

public class SpawnerEggCommand extends SpawnerBaseClass implements Subcommand {

    public SpawnerEggCommand(final LevelledMobs main) {
        super(main);
        startingArgNum = 1;
    }

    final private List<String> allEggOptions = Arrays.asList(
        "/name", "/customdropid", "/entity", "/giveplayer", "/lore", "/minlevel", "/maxlevel",
        "/nolore");

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender,
        final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.spawneregg")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (!VersionUtils.isRunningPaper()) {
            showMessage("command.levelledmobs.spawn_egg.no-paper");
            return;
        }

        if (args.length < 2) {
            showMessage("command.levelledmobs.spawn_egg.usage");
            return;
        }

        boolean hasGivePlayer = false;
        for (int i = 2; i < args.length; i++) {
            if ("/giveplayer".equalsIgnoreCase(args[i])) {
                hasGivePlayer = true;
                break;
            }
        }

        if (!hasGivePlayer && !(sender instanceof Player)) {
            showMessage("command.levelledmobs.spawn_egg.no-player");
            return;
        }

        parseEggCommand(args);
    }

    private void parseEggCommand(final String[] args) {
        hadInvalidArg = false;

        final SpawnerSubCommand.CustomSpawnerInfo info = new SpawnerSubCommand.CustomSpawnerInfo(
            main, messageLabel);
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

        for (int i = 0; i < allEggOptions.size() - 1; i++) {
            final boolean mustBeANumber = (i > 4);
            final String command = allEggOptions.get(i);
            final String foundValue = getArgValue(command, args, mustBeANumber);
            if (hadInvalidArg) {
                return;
            }
            if (Utils.isNullOrEmpty(foundValue)) {
                continue;
            }

            switch (command) {
                case "/name":
                    info.customName = foundValue;
                    break;
                case "/customdropid":
                    info.customDropId = foundValue;
                    break;
                case "/lore":
                    info.customLore = foundValue;
                    break;
                case "/entity":
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.toUpperCase());
                    } catch (final Exception ignored) {
                        commandSender.sendMessage("Invalid spawn type: " + foundValue);
                        return;
                    }
                    break;
                case "/minlevel":
                    info.minLevel = Integer.parseInt(foundValue);
                    break;
                case "/maxlevel":
                    info.maxLevel = Integer.parseInt(foundValue);
                    break;
                case "/giveplayer":
                    if (Utils.isNullOrEmpty(foundValue)) {
                        showMessage("command.levelledmobs.spawn_egg.no-player-specified");
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
                    break;
            }
        }

        if (info.minLevel == -1 || info.maxLevel == -1 || info.spawnType == EntityType.UNKNOWN) {
            showMessage("command.levelledmobs.spawn_egg.no-level-specified");
            return;
        }

        if (info.player == null) {
            showMessage("command.levelledmobs.spawn_egg.no-player");
            return;
        }

        generateEgg(info);
    }

    private void generateEgg(final @NotNull CustomSpawnerInfo info) {
        if (info.customName != null) {
            info.customName = MessageUtils.colorizeAll(info.customName);
        }

        final String materialName = info.spawnType.name() + "_SPAWN_EGG";
        final Material material = Material.getMaterial(materialName);
        if (material == null) {
            // should never see this message:
            commandSender.sendMessage("Invalid material: " + materialName);
            return;
        }
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setMetaItems(meta, info, "LM Spawn Egg");

            meta.getPersistentDataContainer()
                .set(info.main.namespacedKeys.spawnerEgg, PersistentDataType.INTEGER, 1);
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

        final String playerName = VersionUtils.isRunningPaper() ?
            PaperUtils.getPlayerDisplayName(info.player)
            : SpigotUtils.getPlayerDisplayName(info.player);

        final List<String> message = getMessage(
            "command.levelledmobs.spawn_egg.give-message-console",
            new String[]{"%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"},
            new String[]{String.valueOf(info.minLevel), String.valueOf(info.maxLevel), playerName,
                info.spawnType.name()}
        );

        if (!message.isEmpty()) {
            final String consoleMsg = message.get(0)
                .replace(main.configUtils.getPrefix() + " ", "&r");
            Utils.logger.info(consoleMsg);
        }

        showMessage("command.levelledmobs.spawn_egg.give-message",
            new String[]{"%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"},
            new String[]{String.valueOf(info.minLevel), String.valueOf(info.maxLevel), playerName,
                info.spawnType.name()},
            info.player
        );
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main,
        final @NotNull CommandSender sender, @NotNull final String @NotNull [] args) {
        if (!Utils.isNullOrEmpty(args[args.length - 2])) {
            switch (args[args.length - 2].toLowerCase()) {
                case "/entity":
                    final List<String> entityNames = new LinkedList<>();
                    for (final EntityType entityType : EntityType.values()) {
                        entityNames.add(entityType.toString().toLowerCase());
                    }

                    return entityNames;
                case "/giveplayer":
                    final List<String> players = new LinkedList<>();
                    for (final Player player : Bukkit.getOnlinePlayers()) {
                        players.add(player.getName());
                    }
                    players.sort(String.CASE_INSENSITIVE_ORDER);
                    return players;
            }
        }

        return checkTabCompletion(allEggOptions, args);
    }
}
