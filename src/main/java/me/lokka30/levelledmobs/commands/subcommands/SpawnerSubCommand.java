package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SpawnerSubCommand implements Subcommand{
    public SpawnerSubCommand(final LevelledMobs main){
        this.main = main;
    }

    final private LevelledMobs main;

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        final Player player = (Player) sender;

        if (args.length < 2){
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.usage");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        int minLevelFlag = -1;
        int maxLevelFlag = -1;
        int minLevel = -1;
        int maxLevel = -1;
        int customNameFlag = -1;
        int customNameStartFlag = -1;
        int customNameEndFlag = -1;
        int customDropIdFlag = -1;
        String customName = null;
        String customDropId = null;

        for (int i = 1; i < args.length; i++){
            final String arg = args[i].toLowerCase();
            if ("/minlevel".equalsIgnoreCase(arg))
                minLevelFlag = i;
            else if ("/maxlevel".equalsIgnoreCase(arg))
                maxLevelFlag = i;
            else if ("/name".equalsIgnoreCase(arg))
                customNameFlag = i;
            else if (customNameFlag == i - 1 && arg.startsWith("\""))
                customNameStartFlag = i;
            else if (customNameStartFlag > -1 && !arg.startsWith("/") && arg.endsWith("\""))
                customNameEndFlag = i;
            else if ("/customdropid".equalsIgnoreCase(arg))
                customDropIdFlag = i;
        }

        if (minLevelFlag > -1){
            final String value = parseFlagValue(sender, "minlevel", minLevelFlag, args, true, label);
            if (value == null) return;

            minLevel = Integer.parseInt(value);
        }

        if (maxLevelFlag > -1){
            final String value = parseFlagValue(sender, "maxlevel", maxLevelFlag, args, true, label);
            if (value == null) return;

            maxLevel = Integer.parseInt(value);
        }

        if (customNameFlag > -1){

            if (customNameStartFlag > 0 && customNameEndFlag > 0) {
                final StringBuilder sb = new StringBuilder();
                for (int i = customNameStartFlag; i <= customNameEndFlag; i++) {
                    if (i > 0) sb.append(" ");
                    sb.append(args[i].trim());
                }
                customName = sb.toString().trim();
                customName = customName.substring(1, customName.length() -1);
            }
            else
                customName = parseFlagValue(sender, "name", customNameFlag, args, false, label);
            if (customName == null) return;
        }

        if (customDropIdFlag > -1)
            customDropId = parseFlagValue(sender, "customdropid", customDropIdFlag, args, false, label);

        if (minLevel == -1 && maxLevel == -1) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.no-level-specified");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        generateSpawner(player, minLevel, maxLevel, customDropId, customName, label);
    }

    private String parseFlagValue(final CommandSender sender, final String keyName, final int argNumber, final String[] args, final boolean mustBeNumber, final String label){
        if (argNumber + 1 >= args.length || args[argNumber + 1].startsWith("/")){
            List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.no-value");
            message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", label);
            message = Utils.replaceAllInList(message, "%keyname%", keyName);
            message = Utils.colorizeAllInList(message);
            message.forEach(sender::sendMessage);
            return null;
        }

        if (mustBeNumber && !Utils.isInteger(args[argNumber + 1])){
            List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.invalid-value");
            message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", label);
            message = Utils.replaceAllInList(message, "%keyname%", keyName);
            message = Utils.colorizeAllInList(message);
            message.forEach(sender::sendMessage);
            return null;
        }

        return args[argNumber + 1];
    }

    private void generateSpawner(Player player, final int minLevel, final int maxLevel, final String customDropId, final String customName, final String label){
        final ItemStack item = new ItemStack(Material.SPAWNER);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null){
            meta.setDisplayName(customName == null ? "LM spawner" : customName);
            List<String> lore = new LinkedList<>();
            if (minLevel > -1) lore.add("min level: " + minLevel);
            if (maxLevel > -1) lore.add("max level: " + maxLevel);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(main.blockPlaceListener.keySpawner, PersistentDataType.INTEGER, 1);
            meta.getPersistentDataContainer().set(main.blockPlaceListener.keySpawner_MinLevel, PersistentDataType.INTEGER, minLevel);
            meta.getPersistentDataContainer().set(main.blockPlaceListener.keySpawner_MaxLevel, PersistentDataType.INTEGER, maxLevel);
            if (!Utils.isNullOrEmpty(customDropId))
                meta.getPersistentDataContainer().set(main.blockPlaceListener.keySpawner_CustomDropId, PersistentDataType.STRING, customDropId);
            item.setItemMeta(meta);
        }

        int useInvSlotNum = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(useInvSlotNum) != null)
            useInvSlotNum = -1;

        if (useInvSlotNum == -1) {
            for (int i = 0; i <= 35; i++) {
                if (player.getInventory().getItem(i) == null) {
                    useInvSlotNum = i;
                    break;
                }
            }
        }

        if (useInvSlotNum == -1){
            List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.inventory-full");
            message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", label);
            message = Utils.colorizeAllInList(message);
            message.forEach(player::sendMessage);
            return;
        }

        player.getInventory().setItem(useInvSlotNum, item);

        List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.spawner-give-message");
        message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
        message = Utils.replaceAllInList(message, "%label%", label);
        message = Utils.replaceAllInList(message, "%minlevel%", String.valueOf(minLevel));
        message = Utils.replaceAllInList(message, "%maxlevel%", String.valueOf(maxLevel));
        message = Utils.colorizeAllInList(message);
        message.forEach(player::sendMessage);

        message = main.messagesCfg.getStringList("command.levelledmobs.spawner.spawner-give-message-console");
        message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
        message = Utils.replaceAllInList(message, "%label%", label);
        message = Utils.replaceAllInList(message, "%minlevel%", String.valueOf(minLevel));
        message = Utils.replaceAllInList(message, "%maxlevel%", String.valueOf(maxLevel));
        message = Utils.replaceAllInList(message, "%playername%", player.getDisplayName());
        message = Utils.colorizeAllInList(message);
        if (!message.isEmpty()) Utils.logger.info(message.get(0));
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.spawner"))
            return null;

        List<String> CommandsList = new ArrayList<>(Arrays.asList("/minlevel", "/maxlevel", "/name", "/customdropid"));
        boolean inQuotes = false;

        for (int i = 1; i < args.length; i++) {
            final String arg = args[i].toLowerCase();

            if (arg.startsWith("\"") && !arg.endsWith("\""))
                inQuotes = true;
            else if (inQuotes && arg.endsWith("\""))
                inQuotes = false;

            if ("/minlevel".equalsIgnoreCase(arg))
                CommandsList.remove("/minlevel");
            else if ("/maxlevel".equalsIgnoreCase(arg))
                CommandsList.remove("/maxlevel");
            else if ("/name".equalsIgnoreCase(arg))
                CommandsList.remove("/name");
            else if ("/customdropid".equalsIgnoreCase(arg))
                CommandsList.remove("/customdropid");
        }

        final String lastArg = args[args.length - 1];

        if (inQuotes || lastArg.length() > 0 && lastArg.charAt(lastArg.length() - 1) == '\"')
            return Collections.singletonList("");

        return CommandsList;
    }
}
