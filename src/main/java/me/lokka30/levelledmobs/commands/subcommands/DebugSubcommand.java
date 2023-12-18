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
            sender.sendMessage("Options: create / chunk_kill_count / nbt_dump / mylocation / spawn_distance, lew_count");
            return;
        }

        switch (args[1].toLowerCase()){
            case "create" -> createDebugZip(args);
            case "chunk_kill_count" -> chunkKillCount(sender, args);
            case "nbt_dump" -> nbtDump(args);
            case "mylocation" -> showPlayerLocation(sender);
            case "spawn_distance" -> showSpawnDistance(sender, args);
            case "lew_debug" -> showLEWDebug(sender);
            case "lew_clear" -> clearLEWCache(sender);
            case "show_customdrops" -> showCustomDrops();
            // active debugging options:
            case "disable" -> enableOrDisableDebug(false);
            case "disable_after" -> parseDisableAfter(args);
            case "enable" -> enableOrDisableDebug(true);
            case "filter" -> parseFilter(args);
            case "output_to" -> parseOutputTo(args);
            case "status" -> commandSender.sendMessage(main.debugManager.getDebugStatus());
            default -> showMessage("other.create-debug");
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

    private void parseDisableAfter(final String @NotNull [] args){
        if (args.length <= 2){
            commandSender.sendMessage("No value was specified");
            return;
        }

        final String input = args[2];
        if ("0".equals(input) || "none".equalsIgnoreCase(input)){
            main.debugManager.disableAfter = null;
            main.debugManager.disableAfterStr = null;
            return;
        }

        final Long disableAfter = Utils.parseTimeUnit(
                input, null, true, commandSender);

        if (disableAfter != null){
            main.debugManager.disableAfter = disableAfter;
            main.debugManager.disableAfterStr = input;
        }
    }

    private void parseOutputTo(final String @NotNull [] args){
        if (args.length <= 2){
            commandSender.sendMessage("No value was specified");
            return;
        }

        switch (args[2].toLowerCase()){
            case "console" -> main.debugManager.outputType = DebugManager.OutputTypes.CONSOLE;
            case "chat" -> main.debugManager.outputType = DebugManager.OutputTypes.CHAT;
            case "both" -> main.debugManager.outputType = DebugManager.OutputTypes.BOTH;
            default -> commandSender.sendMessage("Invalid option: " + args[2]);
        }

        if (main.debugManager.outputType != DebugManager.OutputTypes.CONSOLE){
            commandSender.sendMessage("WARNING: sending debug messages to chat can cause huge chat spam.");
        }
    }

    private void showCustomDrops(){
        main.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(commandSender);
    }

    private void enableOrDisableDebug(final boolean isEnable){
        final boolean wasEnabled = main.debugManager.isEnabled();

        if (isEnable){
            main.debugManager.enableDebug(commandSender);
            if (wasEnabled)
                commandSender.sendMessage("Debugging is already enabled");
            else
                commandSender.sendMessage("Debugging is now enabled");
        }
        else{
            main.debugManager.disableDebug();
            if (wasEnabled)
                commandSender.sendMessage("Debugging is now disabled");
            else
                commandSender.sendMessage("Debugging is already disabled");
        }
    }

    private void parseFilter(final String @NotNull [] args){
        if (args.length == 2){
            commandSender.sendMessage("Options: entities, rule_names, rule_eval, max_player_dist, players, min_y_level, max_y_level, types");
            return;
        }

        switch (args[2].toLowerCase()){
            case "types" -> parseTypeValues(args, ListTypes.DEBUG);
            case "entities" -> parseTypeValues(args, ListTypes.ENTITY);
            case "rule_names" -> parseTypeValues(args, ListTypes.RULE_NAMES);
            case "rule_eval" -> updateEvaluationType(args);
            case "max_player_dist" -> parseNumberValue(args, NumberSettings.MAX_PLAYERS_DIST);
            case "players" -> parseTypeValues(args, ListTypes.PLAYERS);
            case "min_y_level" -> parseNumberValue(args, NumberSettings.MIN_Y_LEVEL);
            case "max_y_level" -> parseNumberValue(args, NumberSettings.MAX_Y_LEVEL);
        }
    }

    private void parseNumberValue(final String @NotNull [] args, final @NotNull NumberSettings numberSetting){
        if (args.length == 3){
            commandSender.sendMessage("No value was specified");
            return;
        }
        final boolean useNull = "none".equalsIgnoreCase(args[3]);
        try {
            final Integer value = useNull ?
                   null : Integer.parseInt(args[3]);
            switch (numberSetting){
                case MAX_PLAYERS_DIST -> main.debugManager.maxPlayerDistance = value;
                case MIN_Y_LEVEL -> main.debugManager.minYLevel = value;
                case MAX_Y_LEVEL -> main.debugManager.maxYLevel = value;
            }
        }
        catch (Exception ignored){
            commandSender.sendMessage("Invalid number: " + args[3]);
        }
    }

    private void updateEvaluationType(final String @NotNull [] args){
        if (args.length == 3){
            commandSender.sendMessage("No value was specified");
            return;
        }

        try {
            main.debugManager.evaluationType =
                    DebugManager.EvaluationTypes.valueOf(args[3].toUpperCase());
        }
        catch (Exception ignored){
            commandSender.sendMessage("Invalid rule-eval type: " + args[3] + ", valid options are: FAILURE, PASS, BOTH");
        }
    }

    private void parseTypeValues(final String @NotNull [] args, final @NotNull ListTypes listType){
        if (args.length == 3){
            commandSender.sendMessage("Options: add, remove, clear");
            return;
        }

        switch (args[3].toLowerCase()){
            case "clear" -> clearList(listType);
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
        switch (listType){
            case DEBUG -> {
                for (final String debugTypeStr : items){
                    try {
                        final DebugType debugType = DebugType.valueOf(debugTypeStr.toUpperCase());
                        if (isAdd)
                            dm.filterDebugTypes.add(debugType);
                        else
                            dm.filterDebugTypes.remove(debugType);
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
                        if (isAdd)
                            dm.filterEntityTypes.add(entityType);
                        else
                            dm.filterEntityTypes.remove(entityType);
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
                        }
                        else{
                            commandSender.sendMessage("Invalid rule name: " + ruleName);
                        }
                    }
                    else{
                        dm.filterRuleNames.remove(ruleName);
                    }
                }
            }
            case PLAYERS ->{
                // for players we'll allow invalid player names because they might join later
                if (isAdd)
                    dm.filterPlayerNames.addAll(items);
                else {
                    for (final String playerName : items){
                        dm.filterPlayerNames.remove(playerName);
                    }
                }
            }
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
                    "create",
                    "chunk_kill_count",
                    "disable",
                    "disable_after",
                    "enable",
                    "filter",
                    "lew_clear",
                    "lew_debug",
                    "mylocation",
                    "nbt_dump",
                    "output_to",
                    "show_customdrops",
                    "spawn_distance",
                    "status");
        }

        switch (args[1].toLowerCase()){
            case "chunk_kill_count" -> {
                return List.of("reset");
            }
            case "filter" -> {
                return parseFilterTabCompletion(args);
            }
            case "nbt_dump" -> {
                if (args.length == 3) return null;
            }
            case "output_to" -> {
                return List.of("console", "chat", "both");
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<String> parseFilterTabCompletion(final String @NotNull [] args){
        if (args.length == 3){
            return List.of("entities", "min_y_level", "max_y_level", "max_player_dist",
                    "players", "rule_eval", "rule_names", "types");
        }

        if (args.length == 4) {
            switch (args[2].toLowerCase()) {
                case "types", "entities", "rule_names", "players" -> {
                    return List.of("add", "remove", "clear");
                }
                case "rule_eval" -> {
                    return List.of("passed", "failed", "both");
                }
            }
        }

        final boolean isAdd = "add".equalsIgnoreCase(args[3]);
        final boolean isRemove = "remove".equalsIgnoreCase(args[3]);

        if (args.length >= 5 && (isAdd || isRemove)){
            switch (args[2].toLowerCase()) {
                case "types" -> {
                    return getUnusedDebugTypes(isAdd);
                }
                case "entities" -> {
                    return getUnusedEntityTypes(isAdd);
                }
                case "rule_names" -> {
                    return getUnusedRuleNames(isAdd);
                }
                case "players" -> {
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
