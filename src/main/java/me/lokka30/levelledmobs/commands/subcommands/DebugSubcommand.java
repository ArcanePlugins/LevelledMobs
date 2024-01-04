package me.lokka30.levelledmobs.commands.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.DebugCreator;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.nametag.MiscUtils;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            sender.sendMessage("Please select a debug option");
            return;
        }

        final String debugArg = args.length >= 3 ?
                args[2] : null;

        switch (args[1].toLowerCase()){
            case "create-zip" -> createDebugZip(args);
            case "chunk-kill-count" -> chunkKillCount(sender, args);
            case "nbt-dump", "nbt_dump" -> nbtDump(args);
            case "mylocation" -> showPlayerLocation(sender);
            case "spawn-distance" -> showSpawnDistance(sender, args);
            case "lew-debug" -> showLEWDebug(sender);
            case "lew-clear" -> clearLEWCache(sender);
            case "show-customdrops" -> showCustomDrops();
            // active debugging options:
            case "enable" -> enableOrDisableDebug(true, false, debugArg);
            case "enable-all" -> enableOrDisableDebug(true, true, null);
            case "enable-timer" -> parseEnableTimer(args);
            case "disable", "disable-all" -> enableOrDisableDebug(false, false, null);
            case "filter-results" -> parseFilter(args);
            case "output-debug" -> parseOutputTo(args);
            case "view-debug-status" -> commandSender.sendMessage(main.debugManager.getDebugStatus());
            default -> commandSender.sendMessage("Please enter a debug option.");
        }
    }

    private void createDebugZip(final String @NotNull [] args){
        if (args.length >= 3 && "confirm".equalsIgnoreCase(args[2])) {
            DebugCreator.createDebug(main, commandSender);
        } else {
            showMessage("other.create-debug");
        }
    }

    private void nbtDump(final String @NotNull [] args){
        if (!main.nametagQueueManager.nametagSenderHandler.versionInfo.isNMSVersionValid()){
            commandSender.sendMessage("Unable to dump, an unknown NMS version was detected");
            return;
        }
        doNbtDump(commandSender, args);
        if (!(commandSender instanceof ConsoleCommandSender)) {
            commandSender.sendMessage("NBT data has been written to the console");
        }
    }

    private void parseEnableTimer(final String @NotNull [] args){
        if (args.length <= 2){
            commandSender.sendMessage("No value was specified");
            return;
        }

        final String input = args[2];
        if ("0".equals(input) || "none".equalsIgnoreCase(input)){
            main.debugManager.disableAfter = null;
            main.debugManager.disableAfterStr = null;
            main.debugManager.timerWasChanged(false);
            commandSender.sendMessage("Debug timer disabled");
            return;
        }

        final Long disableAfter = Utils.parseTimeUnit(
                input, null, true, commandSender);

        if (disableAfter != null){
            if (args.length >= 4){
                if (!parseEnableDebugCategory(args[3])) return;
            }
            main.debugManager.disableAfter = disableAfter;
            main.debugManager.disableAfterStr = input;
            commandSender.sendMessage("Debug enabled for " + input);
            if (main.debugManager.isEnabled())
                main.debugManager.timerWasChanged(true);
            else
                main.debugManager.enableDebug(commandSender, true, false);
        }
    }

    private void parseOutputTo(final String @NotNull [] args){
        if (args.length <= 2){
            commandSender.sendMessage("Current value: " + main.debugManager.outputType.name(
                ).toLowerCase().replace("_", "-"));
            return;
        }
        boolean wasInvalid = false;

        switch (args[2].toLowerCase()){
            case "to-console" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CONSOLE;
            case "to-chat" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CHAT;
            case "to-both" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_BOTH;
            default -> {
                commandSender.sendMessage("Invalid option: " + args[2]);
                wasInvalid = true;
            }
        }
        if (!wasInvalid)
            commandSender.sendMessage("Output-debug updated to " + main.debugManager.outputType.name(
                ).replace("_", "-").toLowerCase());

        if (main.debugManager.outputType != DebugManager.OutputTypes.TO_CONSOLE){
            commandSender.sendMessage("WARNING: sending debug messages to chat can cause huge chat spam.");
        }
    }

    private void showCustomDrops(){
        main.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(commandSender);
    }

    private void enableOrDisableDebug(final boolean isEnable, final boolean isEnableAll, final @Nullable String debugCategory){
        final boolean wasEnabled = main.debugManager.isEnabled();

        if (isEnable){
            final boolean wasTimerEnabled = main.debugManager.getIsTimerEnabled();
            final boolean enableAllChanged = main.debugManager.isBypassAllFilters() != isEnableAll;
            if (debugCategory != null) {
                if (!parseEnableDebugCategory(debugCategory)) return;
            }
            main.debugManager.enableDebug(commandSender, false, isEnableAll);
            if (wasEnabled && !enableAllChanged){
                if (wasTimerEnabled)
                    commandSender.sendMessage("Debugging is already enabled, disabled timer");
                else
                    commandSender.sendMessage("Debugging is already enabled");
            }
            else {
                if (isEnableAll)
                    commandSender.sendMessage("All debug options enabled");
                else
                    commandSender.sendMessage("Debugging is now enabled");
            }
        }
        else{
            main.debugManager.disableDebug();
            if (wasEnabled)
                commandSender.sendMessage("Debugging is now disabled");
            else
                commandSender.sendMessage("Debugging is already disabled");
        }
    }

    private boolean parseEnableDebugCategory(final @NotNull String debugCategory){
        DebugType debugType;
        try{
            debugType = DebugType.valueOf(debugCategory.toUpperCase());
        }
        catch (Exception ignored){
            commandSender.sendMessage("Invalid debug type: " + debugCategory);
            return false;
        }

        main.debugManager.filterDebugTypes.clear();
        main.debugManager.filterDebugTypes.add(debugType);
        commandSender.sendMessage("Debug type set to " + debugCategory);
        return true;
    }

    private void parseFilter(final String @NotNull [] args){
        if (args.length == 2){
            commandSender.sendMessage("Please enter a filter option");
            return;
        }

        switch (args[2].toLowerCase()){
            case "set-debug" -> parseTypeValues(args, ListTypes.DEBUG);
            case "set-entities" -> parseTypeValues(args, ListTypes.ENTITY);
            case "set-rules" -> parseTypeValues(args, ListTypes.RULE_NAMES);
            case "listen-for" -> updateEvaluationType(args);
            case "set-distance-from-players" -> parseNumberValue(args, NumberSettings.MAX_PLAYERS_DIST);
            case "set-players" -> parseTypeValues(args, ListTypes.PLAYERS);
            case "set-y-height" -> parseYHeight(args);
            case "clear-all-filters" -> resetFilters();
        }
    }

    private void parseYHeight(final String @NotNull [] args){
        if (args.length <= 3){
            if (main.debugManager.minYLevel == null && main.debugManager.maxYLevel == null)
                commandSender.sendMessage("Please set a min and/or max y-height, or clear the filter");
            else
                commandSender.sendMessage("min-y-height: " + main.debugManager.minYLevel +
                        ", max-y-height: " + main.debugManager.maxYLevel);
            return;
        }

        switch (args[3].toLowerCase()){
            case "min-y-height" -> parseNumberValue(args, NumberSettings.MIN_Y_LEVEL);
            case "max-y-height" -> parseNumberValue(args, NumberSettings.MAX_Y_LEVEL);
            case "clear" -> {
                main.debugManager.minYLevel = null;
                main.debugManager.maxYLevel = null;
                commandSender.sendMessage("All y-height filters cleared");
            }
            default -> commandSender.sendMessage("Invalid option");
        }
    }

    private void resetFilters(){
        main.debugManager.resetFilters();
        commandSender.sendMessage("All filters have been cleared");
    }

    private void parseNumberValue(final String @NotNull [] args, final @NotNull NumberSettings numberSetting){
        final int argNumber = numberSetting == NumberSettings.MAX_PLAYERS_DIST ?
                3 : 4;

        if (args.length == argNumber){
            commandSender.sendMessage("No value was specified");
            return;
        }

        final boolean useNull = "none".equalsIgnoreCase(args[argNumber]);
        try {
            final Integer value = useNull ?
                   null : Integer.parseInt(args[argNumber]);
            switch (numberSetting){
                case MAX_PLAYERS_DIST -> {
                    main.debugManager.maxPlayerDistance = value;
                    commandSender.sendMessage("Distance from players set to " + value);
                }
                case MIN_Y_LEVEL -> {
                    main.debugManager.minYLevel = value;
                    commandSender.sendMessage("Min y-height set to " + value);
                }
                case MAX_Y_LEVEL -> {
                    main.debugManager.maxYLevel = value;
                    commandSender.sendMessage("Max y-height set to " + value);
                }
            }
        }
        catch (Exception ignored){
            commandSender.sendMessage("Invalid number: " + args[argNumber]);
        }
    }

    private void updateEvaluationType(final String @NotNull [] args){
        if (args.length == 3){
            commandSender.sendMessage("Current value: " + main.debugManager.listenFor);
            return;
        }

        try {
            main.debugManager.listenFor =
                    DebugManager.ListenFor.valueOf(args[3].toUpperCase());
            switch (main.debugManager.listenFor){
                case BOTH -> commandSender.sendMessage("Listening for all debug notice events");
                case FAILURE -> commandSender.sendMessage("Listening for failed debug notice events");
                case SUCCESS -> commandSender.sendMessage("Listening for successful debug notice events");
            }
        }
        catch (Exception ignored){
            commandSender.sendMessage("Invalid listen-for type: " + args[3] + ", valid options are: failure, success, both");
        }
    }

    private void parseTypeValues(final String @NotNull [] args, final @NotNull ListTypes listType){
        if (args.length == 3){
            viewList(listType);
            return;
        }

        switch (args[3].toLowerCase()){
            case "clear" -> {
                clearList(listType);
                String listTypeMsg = null;
                switch (listType){
                    case PLAYERS -> listTypeMsg = "Players";
                    case RULE_NAMES -> listTypeMsg = "Rule names";
                    case ENTITY -> listTypeMsg = "Entity types";
                    case DEBUG ->  listTypeMsg = "Debug types";
                }
                commandSender.sendMessage("All filters cleared for " + listTypeMsg);
            }
            case "add", "remove" -> {
                if (args.length == 4){
                    commandSender.sendMessage("No values were specified for " + args[3]);
                    return;
                }
                final Set<String> items = new HashSet<>(
                        Arrays.asList(args).subList(4, args.length));
                addOrRemoveItemsToList("add".equalsIgnoreCase(args[3]), items, listType);
            }
            default -> commandSender.sendMessage("Invalid option: " + args[3]);
        }
    }

    private void viewList(final @NotNull ListTypes listType){
        Set<?> useList;
        switch (listType){
            case DEBUG -> useList = main.debugManager.filterDebugTypes;
            case ENTITY -> useList = main.debugManager.filterEntityTypes;
            case RULE_NAMES -> useList = main.debugManager.filterRuleNames;
            case PLAYERS -> useList = main.debugManager.filterPlayerNames;
            default -> {
                Utils.logger.error("View not defined for listtype: " + listType);
                return;
            }
        }

        final String msg = useList.isEmpty() ?
                "No values currently defined" : useList.toString();
        commandSender.sendMessage(msg);
    }

    private void clearList(final @NotNull ListTypes listType){
        switch (listType){
            case DEBUG -> main.debugManager.filterDebugTypes.clear();
            case ENTITY -> main.debugManager.filterEntityTypes.clear();
            case RULE_NAMES -> main.debugManager.filterRuleNames.clear();
            case PLAYERS -> main.debugManager.filterPlayerNames.clear();
        }
    }

    private void addOrRemoveItemsToList(final boolean isAdd, final @NotNull Set<String> items,
                                        final @NotNull ListTypes listType){
        final DebugManager dm = main.debugManager;
        final List<String> optionsAddedOrRemoved = new LinkedList<>();
        switch (listType){
            case DEBUG -> {
                for (final String debugTypeStr : items){
                    try {
                        final DebugType debugType = DebugType.valueOf(debugTypeStr.toUpperCase());
                        if (isAdd) {
                            dm.filterDebugTypes.add(debugType);
                            optionsAddedOrRemoved.add(debugType.name());
                        }
                        else {
                            dm.filterDebugTypes.remove(debugType);
                            optionsAddedOrRemoved.add(debugType.name());
                        }
                    }
                    catch (Exception ignored){
                        if (isAdd) commandSender.sendMessage("Invalid debug type: " + debugTypeStr);
                    }
                }
            }
            case ENTITY -> {
                for (final String entityTypeStr : items){
                    try {
                        final EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase());
                        if (isAdd) {
                            dm.filterEntityTypes.add(entityType);
                            optionsAddedOrRemoved.add(entityType.name());
                        }
                        else {
                            dm.filterEntityTypes.remove(entityType);
                            optionsAddedOrRemoved.add(entityType.name());
                        }
                    }
                    catch (Exception ignored){
                        if (isAdd) commandSender.sendMessage("Invalid entity type: " + entityTypeStr);
                    }
                }
            }
            case RULE_NAMES -> {
                final Set<String> allRuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules(false)) {
                    allRuleNames.add(ruleInfo.getRuleName().replace(" ", "_"));
                }

                for (final String ruleName : items){
                    if (isAdd){
                        String actualRuleName = null;
                        for (final String foundRuleName : allRuleNames) {
                            if (foundRuleName.equalsIgnoreCase(ruleName)) {
                                actualRuleName = foundRuleName;
                                break;
                            }
                        }
                        if (actualRuleName != null){
                            dm.filterRuleNames.add(actualRuleName.replace(" ", "_"));
                            optionsAddedOrRemoved.add(actualRuleName);
                        }
                        else{
                            commandSender.sendMessage("Invalid rule name: " + ruleName);
                        }
                    }
                    else{
                        dm.filterRuleNames.remove(ruleName);
                        optionsAddedOrRemoved.add(ruleName);
                    }
                }
            }
            case PLAYERS ->{
                // for players we'll allow invalid player names because they might join later
                if (isAdd) {
                    dm.filterPlayerNames.addAll(items);
                    optionsAddedOrRemoved.addAll(items);
                }
                else {
                    for (final String playerName : items){
                        dm.filterPlayerNames.remove(playerName);
                        optionsAddedOrRemoved.add(playerName);
                    }
                }
            }
        }

        if (!optionsAddedOrRemoved.isEmpty()){
            final String useName = listType.name().replace("_", " ").toLowerCase();
            if (isAdd)
                commandSender.sendMessage("Added values to " + useName + " : " + optionsAddedOrRemoved);
            else
                commandSender.sendMessage("Removed values to " + useName + ": " + optionsAddedOrRemoved);
        }
    }

    private enum NumberSettings{
        MAX_PLAYERS_DIST, MIN_Y_LEVEL, MAX_Y_LEVEL
    }

    private enum ListTypes{
        DEBUG, ENTITY, RULE_NAMES, PLAYERS
    }

    private void showLEWDebug(final @NotNull CommandSender sender){
        if (!sender.hasPermission("levelledmobs.command.debug.lew_debug")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        final String result = LivingEntityWrapper.getLEWDebug();
        sender.sendMessage(result);
    }

    private void clearLEWCache(final @NotNull CommandSender sender){
        if (!sender.hasPermission("levelledmobs.command.debug.lew_clear")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        LivingEntityWrapper.clearCache();
        sender.sendMessage("Cleared the LEW cache");
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
            return List.of(
                    "enable-all",
                    "enable-timer",
                    "enable",
                    "disable",
                    "filter-results",
                    "output-debug",
                    "view-debug-status",
                    "create-zip",
                    "show-customdrops",
                    "chunk-kill-count",
                    "mylocation",
                    "spawn-distance",
                    "lew-debug",
                    "lew-clear",
                    "nbt-dump"
            );
        }

        switch (args[1].toLowerCase()){
            case "enable" -> {
                if (args.length == 3) return getDebugTypes();
            }
            case "enable-timer" -> {
                if (args.length == 4) return getDebugTypes();
            }
            case "chunk-kill-count" -> {
                return List.of("reset");
            }
            case "filter-results" -> {
                return parseFilterTabCompletion(args);
            }
            case "nbt-dump" -> {
                if (args.length == 3) return null;
            }
            case "output-debug" -> {
                final List<String> values = new LinkedList<>();
                for (final DebugManager.OutputTypes outputTypes : DebugManager.OutputTypes.values()){
                    if (main.debugManager.outputType != outputTypes)
                        values.add(outputTypes.name().replace("_", "-").toLowerCase());
                }
                return values;
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<String> getDebugTypes(){
        final List<String> list = new LinkedList<>();
        for (final DebugType debugType : DebugType.values()){
            list.add(debugType.toString().toLowerCase());
        }
        return list;
    }

    private @NotNull List<String> parseFilterTabCompletion(final String @NotNull [] args){
        if (args.length == 3){
            return List.of("clear-all-filters", "set-entities", "set-y-height", "set-distance-from-players",
                    "set-players", "listen-for", "set-rules", "set-debug");
        }

        if (args.length == 4 && "set-y-height".equalsIgnoreCase(args[2])){
            return List.of("min-y-height", "max-y-height", "clear");
        }

        if (args.length == 4) {
            switch (args[2].toLowerCase()) {
                case "set-debug", "set-entities", "set-rules", "set-players" -> {
                    return List.of("add", "remove", "clear");
                }
                case "listen-for" -> {
                    final List<String> values = new LinkedList<>();
                    for (final DebugManager.ListenFor evaluationType : DebugManager.ListenFor.values()){
                        if (main.debugManager.listenFor != evaluationType)
                            values.add(evaluationType.name().toLowerCase());
                    }
                    return values;
                }
            }
        }

        final boolean isAdd = "add".equalsIgnoreCase(args[3]);
        final boolean isRemove = "remove".equalsIgnoreCase(args[3]);

        if (args.length >= 5 && (isAdd || isRemove)){
            switch (args[2].toLowerCase()) {
                case "set-debug" -> {
                    return getUnusedDebugTypes(isAdd);
                }
                case "set-entities" -> {
                    return getUnusedEntityTypes(isAdd);
                }
                case "set-rules" -> {
                    return getUnusedRuleNames(isAdd);
                }
                case "set-players" -> {
                    return getUnusedPlayers(isAdd);
                }
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<String> getUnusedDebugTypes(final boolean isAdd){
        final List<String> debugs = new LinkedList<>();
        if (isAdd){
            for (final DebugType debugType : DebugType.values()){
                debugs.add(debugType.toString().toLowerCase());
            }
        }

        for (final DebugType debugType : main.debugManager.filterDebugTypes){
            if (isAdd)
                debugs.remove(debugType.toString().toLowerCase());
            else
                debugs.add(debugType.toString().toLowerCase());
        }

        return debugs;
    }

    private @NotNull List<String> getUnusedEntityTypes(final boolean isAdd){
        final List<String> et = new LinkedList<>();
        if (isAdd){
            for (final EntityType entityType : EntityType.values()){
                if (main.debugManager.excludedEntityTypes.contains(entityType.name())) continue;
                et.add(entityType.toString().toLowerCase());
            }
        }

        for (final EntityType entityType : main.debugManager.filterEntityTypes){
            if (isAdd)
                et.remove(entityType.toString().toLowerCase());
            else
                et.add(entityType.toString().toLowerCase());
        }

        return et;
    }

    private @NotNull List<String> getUnusedRuleNames(final boolean isAdd){
        final Set<String> ruleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (isAdd){
            for (final RuleInfo ri : main.rulesParsingManager.getAllRules(false)){
                ruleNames.add(ri.getRuleName().replace(" ", "_"));
            }
        }

        for (final String ruleName : main.debugManager.filterRuleNames){
            if (isAdd)
                ruleNames.remove(ruleName);
            else
                ruleNames.add(ruleName);
        }

        return new ArrayList<>(ruleNames);
    }

    private @NotNull List<String> getUnusedPlayers(final boolean isAdd){
        final Set<String> players = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (isAdd) {
            for (final Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
        }

        for (final String playerName : main.debugManager.filterPlayerNames){
            if (isAdd)
                players.remove(playerName);
            else
                players.add(playerName);
        }

        return new ArrayList<>(players);
    }
}
