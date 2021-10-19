/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.DoNotMerge;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Gives the user a specialized spawner that only spawns mobs within certain level
 * criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
public class SpawnerSubCommand implements Subcommand{

    public SpawnerSubCommand(final LevelledMobs main) {this.main = main;}

    final private LevelledMobs main;
    final private List<String> allSpawnerOptions = Arrays.asList(
            "/name", "/customdropid", "/spawntype", "/giveplayer", "/lore", "/minlevel", "/maxlevel", "/delay",
            "/maxnearbyentities", "/minspawndelay", "/maxspawndelay", "/requiredplayerrange",
            "/spawncount", "/spawnrange", "/nolore"
    );
    private boolean hadInvalidArg;

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length < 2){
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.usage");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
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
            if (operationEnum == OperationEnum.CREATE)
                sender.sendMessage("Command can only be run by a player unless /giveplayer is specified");
            else
                sender.sendMessage("Command can only be run by a player");
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
            List<String> messages;
            if (main.companion.spawner_InfoIds.contains(playerId))
                messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.info.status-enabled");
            else
                messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.info.status-not-enabled");

            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if ("on".equalsIgnoreCase(args[2])){
            if (main.companion.spawner_CopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(sender, playerId, label);
            }

            main.companion.spawner_InfoIds.add(playerId);
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.info.enabled");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }
        else if ("off".equalsIgnoreCase(args[2]))
            infoGotDisabled(sender, playerId, label);

        checkListener();
    }

    private void parseCopyCommand(@NotNull final CommandSender sender, final String label, final String[] args){
        if (!sender.hasPermission("levelledmobs.command.spawner.copy")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        final UUID playerId = ((Player) sender).getUniqueId();

        if (args.length == 2){
            List<String> messages;
            if (main.companion.spawner_CopyIds.contains(playerId))
                messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.copy.status-enabled");
            else
                messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.copy.status-not-enabled");

            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);

            return;
        }

        if ("on".equalsIgnoreCase(args[2])){
            if (main.companion.spawner_InfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(sender, playerId, label);
            }

            main.companion.spawner_CopyIds.add(playerId);
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.copy.enabled");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
        }
        else if ("off".equalsIgnoreCase(args[2]))
            copyGotDisabled(sender, playerId, label);

        checkListener();
    }

    private void copyGotDisabled(final @NotNull CommandSender sender, final UUID playerId, final String label){
        main.companion.spawner_CopyIds.remove(playerId);
        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.copy.disabled");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    private void infoGotDisabled(final @NotNull CommandSender sender, final UUID playerId, final String label){
        main.companion.spawner_InfoIds.remove(playerId);
        List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.info.disabled");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%", label);
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }


    private void checkListener(){
        final boolean needsListener = (!main.companion.spawner_InfoIds.isEmpty() || !main.companion.spawner_CopyIds.isEmpty());
        if (needsListener && !main.companion.playerInteractListenerIsRegistered){
            main.companion.playerInteractListenerIsRegistered = true;
            Bukkit.getPluginManager().registerEvents(main.playerInteractEventListener, main);
        }
        else if (!needsListener && main.companion.playerInteractListenerIsRegistered){
            main.companion.playerInteractListenerIsRegistered = false;
            HandlerList.unregisterAll(main.playerInteractEventListener);
        }
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
            final String foundValue = getArgValue(command, args, sender, label, mustBeANumber);
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
                        sender.sendMessage("No player was specified");
                        return;
                    }
                    try { info.player = Bukkit.getPlayer(foundValue); }
                    catch (Exception e){
                        sender.sendMessage("Invalid or offline player: " + foundValue);
                        return;
                    }
                    if (info.player == null){
                        sender.sendMessage("Invalid or offline player: " + foundValue);
                        return;
                    }
                    break;
            }
        }

        if (info.minLevel == -1 && info.maxLevel == -1) {
            List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.spawner.no-level-specified");
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
            messages = Utils.replaceAllInList(messages, "%label%", label);
            messages = Utils.colorizeAllInList(messages);
            messages.forEach(sender::sendMessage);
            return;
        }

        if (info.player == null){
            sender.sendMessage("No player was specified");
            return;
        }

        generateSpawner(info);
    }

    @Nullable
    private String getArgValue(final String key, final String @NotNull [] args, final CommandSender sender, final String label, final boolean mustBeNumber){
        int keyFlag = -1;
        int nameStartFlag = - 1;
        int nameEndFlag = - 1;

        for (int i = 2; i < args.length; i++){
            final String arg = args[i];
            if (key.equalsIgnoreCase(arg))
                keyFlag = i;
            else if (keyFlag == i - 1 && arg.startsWith("\""))
                nameStartFlag = i;
            else if (nameStartFlag > -1 && !arg.startsWith("/") && arg.endsWith("\""))
                nameEndFlag = i;
        }

        if (keyFlag < 0) return null;
        String keyValue;

        if (nameEndFlag > 0) {
            final StringBuilder sb = new StringBuilder();
            for (int i = nameStartFlag; i <= nameEndFlag; i++) {
                if (i > 0) sb.append(" ");
                sb.append(args[i].trim());
            }
            keyValue = sb.toString().trim();
            keyValue = keyValue.substring(1, keyValue.length() - 1);
        }
        else
            keyValue = parseFlagValue(sender, key, keyFlag, args, mustBeNumber, label);

        return keyValue;
    }

    private @Nullable String parseFlagValue(final CommandSender sender, final String keyName, final int argNumber, final String @NotNull [] args, final boolean mustBeNumber, final String label){
        if (argNumber + 1 >= args.length || args[argNumber + 1].startsWith("/")){
            List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.no-value");
            message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", label);
            message = Utils.replaceAllInList(message, "%keyname%", keyName);
            message = Utils.colorizeAllInList(message);
            message.forEach(sender::sendMessage);
            hadInvalidArg = true;
            return null;
        }

        if (mustBeNumber && !Utils.isInteger(args[argNumber + 1])){
            List<String> message = main.messagesCfg.getStringList("command.levelledmobs.spawner.invalid-value");
            message = Utils.replaceAllInList(message, "%prefix%", main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", label);
            message = Utils.replaceAllInList(message, "%keyname%", keyName);
            message = Utils.colorizeAllInList(message);
            message.forEach(sender::sendMessage);
            hadInvalidArg = true;
            return null;
        }

        return args[argNumber + 1];
    }

    public static void generateSpawner(final @NotNull CustomSpawnerInfo info){
        if (info.maxSpawnDelay != null && (info.minSpawnDelay == null || info.minSpawnDelay > info.maxSpawnDelay)) {
            // settting max spawn delay lower than min spawn delay will result in an exception
            info.minSpawnDelay = info.maxSpawnDelay;
        }

        if (info.minSpawnDelay != null && (info.maxSpawnDelay == null || info.maxSpawnDelay > info.minSpawnDelay)) {
            // settting min spawn delay higher than max spawn delay will result in an exception
            info.maxSpawnDelay = info.minSpawnDelay;
        }

        final ItemStack item = new ItemStack(Material.SPAWNER);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null){
            meta.setDisplayName(info.customName == null ? "LM spawner" : info.customName);
            List<String> lore = new LinkedList<>();

            try {
                int itemsCount = 0;
                final StringBuilder loreLine = new StringBuilder();
                for (final Field f : info.getClass().getDeclaredFields()) {
                    if (!Modifier.isPublic(f.getModifiers())) continue;
                    if (f.get(info) == null) continue;
                    final String name = f.getName();
                    if (f.isAnnotationPresent(DoNotMerge.class)) continue;

                    if ("-1".equals(f.get(info).toString()) && (name.equals("minLevel") || name.equals("maxLevel")))
                        continue;

                    if (itemsCount > 2){
                        lore.add(loreLine.toString());
                        loreLine.setLength(0);
                        itemsCount = 0;
                    }

                    if (loreLine.length() > 0) loreLine.append(", ");
                    loreLine.append(String.format("&7%s: &b%s&7", name, f.get(info)));
                    itemsCount++;
                }
                if (itemsCount > 0)
                    lore.add(loreLine.toString());
            }
            catch (Exception e){
                e.printStackTrace();
            }

            if (!info.noLore && info.lore == null && info.customLore == null) {
                lore = Utils.colorizeAllInList(lore);
                meta.setLore(lore);

                final StringBuilder sbLore = new StringBuilder();
                for (final String loreLine : lore) {
                    if (sbLore.length() > 0) sbLore.append("\n");
                    sbLore.append(loreLine);
                }
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING, sbLore.toString());
            }
            else if (!info.noLore || info.customLore != null){
                final String useLore = info.customLore == null ?
                        info.lore : MessageUtils.colorizeAll(info.customLore).replace("\\n", "\n");

                lore.clear();
                lore.addAll(List.of(useLore.split("\n")));
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING, useLore);
            }

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
            List<String> message = info.main.messagesCfg.getStringList("command.levelledmobs.spawner.inventory-full");
            message = Utils.replaceAllInList(message, "%prefix%", info.main.configUtils.getPrefix());
            message = Utils.replaceAllInList(message, "%label%", info.label);
            message = Utils.colorizeAllInList(message);
            message.forEach(info.player::sendMessage);
            return;
        }

        info.player.getInventory().setItem(useInvSlotNum, item);

        List<String> message = info.main.messagesCfg.getStringList("command.levelledmobs.spawner.spawner-give-message");
        message = Utils.replaceAllInList(message, "%prefix%", info.main.configUtils.getPrefix());
        message = Utils.replaceAllInList(message, "%label%", info.label);
        message = Utils.colorizeAllInList(message);
        message.forEach(info.player::sendMessage);

        message = info.main.messagesCfg.getStringList("command.levelledmobs.spawner.spawner-give-message-console");
        message = Utils.replaceAllInList(message, "%prefix%", info.main.configUtils.getPrefix());
        message = Utils.replaceAllInList(message, "%label%", info.label);
        message = Utils.replaceAllInList(message, "%minlevel%", String.valueOf(info.minLevel));
        message = Utils.replaceAllInList(message, "%maxlevel%", String.valueOf(info.maxLevel));
        message = Utils.replaceAllInList(message, "%playername%", info.player.getDisplayName());
        message = Utils.colorizeAllInList(message);
        if (!message.isEmpty()) Utils.logger.info(message.get(0));
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

        final Set<String> commandsList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        commandsList.addAll(allSpawnerOptions);

        boolean inQuotes = false;

        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];

            if (arg.startsWith("\"") && !arg.endsWith("\""))
                inQuotes = true;
            else if (inQuotes && arg.endsWith("\""))
                inQuotes = false;

            commandsList.remove(arg);
        }

        final String lastArg = args[args.length - 1];

        if (inQuotes || lastArg.length() > 0 && lastArg.charAt(lastArg.length() - 1) == '\"')
            return Collections.emptyList();

        final List<String> result = new ArrayList<>(commandsList.size());
        result.addAll(commandsList);
        return result;
    }

    public static class CustomSpawnerInfo{
        public CustomSpawnerInfo(final LevelledMobs main, final String label){
            this.main = main;
            this.label = label;
            this.minLevel = -1;
            this.maxLevel = -1;
            this.spawnType = EntityType.UNKNOWN;
        }

        @DoNotMerge
        final public LevelledMobs main;
        @DoNotMerge
        final public String label;
        @DoNotMerge
        public Player player;
        public int minLevel;
        public int maxLevel;
        @DoNotMerge
        public boolean noLore;
        public Integer delay;
        public Integer maxNearbyEntities;
        public Integer minSpawnDelay;
        public Integer maxSpawnDelay;
        public Integer requiredPlayerRange;
        public Integer spawnCount;
        public Integer spawnRange;
        public String customDropId;
        @DoNotMerge
        public String customName;
        public EntityType spawnType;
        @DoNotMerge
        public String customLore;
        public String lore;
    }

    private enum OperationEnum{
        CREATE,
        COPY,
        INFO
    }
}
