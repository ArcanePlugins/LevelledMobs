package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.util.LinkedList
import java.util.Locale
import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.debug.DebugCreator
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.nametag.MiscUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

/**
 * Parses commands for various debug stuff
 *
 * @author stumper66
 * @since 3.2.0
 */
class DebugSubcommand: MessagesBase(), Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ){
        commandSender = sender
        messageLabel = label

        if (!sender.hasPermission("levelledmobs.command.debug")) {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
            return
        }

        if (args.size <= 1) {
            sender.sendMessage("Please select a debug option")
            return
        }

        val debugArg = if (args.size >= 3) args[2] else null

        when (args[1].lowercase(Locale.getDefault())) {
            "create-zip" -> createDebugZip(args)
            "chunk-kill-count" -> chunkKillCount(sender, args)
            "nbt-dump", "nbt_dump" -> nbtDump(args)
            "mylocation" -> showPlayerLocation(sender)
            "spawn-distance" -> showSpawnDistance(sender, args)
            "lew-debug" -> showLEWDebug(sender)
            "lew-clear" -> clearLEWCache(sender)
            "show-customdrops" -> showCustomDrops()
            "enable" -> enableOrDisableDebug(isEnable = true, isEnableAll = false, debugCategory = debugArg)
            "enable-all" -> enableOrDisableDebug(isEnable = true, isEnableAll = true, debugCategory = null)
            "enable-timer" -> parseEnableTimer(args)
            "disable", "disable-all" -> enableOrDisableDebug(
                isEnable = false,
                isEnableAll = false,
                debugCategory = null
            )
            "filter-results" -> parseFilter(args)
            "output-debug" -> parseOutputTo(args)
            "view-debug-status" -> commandSender!!.sendMessage(LevelledMobs.instance.debugManager.getDebugStatus())
            else -> commandSender!!.sendMessage("Please enter a debug option.")
        }
    }

    private fun createDebugZip(args: Array<String>) {
        if (args.size >= 3 && "confirm".equals(args[2], ignoreCase = true)) {
            DebugCreator.createDebug(commandSender!!)
        } else {
            showMessage("other.create-debug")
        }
    }

    private fun nbtDump(args: Array<String>) {
        if (!LevelledMobs.instance.ver.isNMSVersionValid) {
            commandSender!!.sendMessage("Unable to dump, an unknown NMS version was detected")
            return
        }
        doNbtDump(commandSender!!, args)
        if (commandSender !is ConsoleCommandSender) {
            commandSender!!.sendMessage("NBT data has been written to the console")
        }
    }

    private fun parseEnableTimer(args: Array<String>) {
        if (args.size <= 2) {
            commandSender!!.sendMessage("No value was specified")
            return
        }

        val main = LevelledMobs.instance
        val input = args[2]
        if ("0" == input || "none".equals(input, ignoreCase = true)) {
            main.debugManager.disableAfter = null
            main.debugManager.disableAfterStr = null
            main.debugManager.timerWasChanged(false)
            commandSender!!.sendMessage("Debug timer disabled")
            return
        }

        var disableAfter = Utils.parseTimeUnit(
            input, null, true, commandSender
        )

        if (disableAfter != null) {
            disableAfter *= 1000
            if (args.size >= 4) {
                if (!parseEnableDebugCategory(args[3])) return
            }
            main.debugManager.disableAfter = disableAfter
            main.debugManager.disableAfterStr = input
            commandSender!!.sendMessage("Debug enabled for $input")
            if (main.debugManager.isEnabled) main.debugManager.timerWasChanged(true)
            else main.debugManager.enableDebug(commandSender!!, usetimer = true, bypassFilters = false)
        }
    }

    private fun parseOutputTo(args: Array<String>) {
        val main = LevelledMobs.instance
        if (args.size <= 2) {
            commandSender!!.sendMessage(
                "Current value: " + main.debugManager.outputType.name.lowercase().replace("_", "-")
            )
            return
        }
        var wasInvalid = false

        when (args[2].lowercase(Locale.getDefault())) {
            "to-console" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CONSOLE
            "to-chat" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CHAT
            "to-both" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_BOTH
            else -> {
                commandSender!!.sendMessage("Invalid option: " + args[2])
                wasInvalid = true
            }
        }
        if (!wasInvalid) commandSender!!.sendMessage(
            "Output-debug updated to " + main.debugManager.outputType.name.replace("_", "-")
                .lowercase(Locale.getDefault())
        )

        if (main.debugManager.outputType !== DebugManager.OutputTypes.TO_CONSOLE) {
            commandSender!!.sendMessage("WARNING: sending debug messages to chat can cause huge chat spam.")
        }
    }

    private fun showCustomDrops() {
        LevelledMobs.instance.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(commandSender!!)
    }

    private fun enableOrDisableDebug(
        isEnable: Boolean,
        isEnableAll: Boolean,
        debugCategory: String?
    ){
        val main = LevelledMobs.instance
        val wasEnabled = main.debugManager.isEnabled

        if (isEnable) {
            val wasTimerEnabled = main.debugManager.isTimerEnabled
            val enableAllChanged = main.debugManager.bypassAllFilters != isEnableAll
            if (debugCategory != null) {
                if (!parseEnableDebugCategory(debugCategory)) return
            }
            main.debugManager.enableDebug(commandSender!!, false, isEnableAll)
            if (wasEnabled && !enableAllChanged) {
                if (wasTimerEnabled) commandSender!!.sendMessage("Debugging is already enabled, disabled timer")
                else commandSender!!.sendMessage("Debugging is already enabled")
            } else {
                if (isEnableAll) commandSender!!.sendMessage("All debug options enabled")
                else commandSender!!.sendMessage("Debugging is now enabled")
            }
        } else {
            main.debugManager.disableDebug()
            if (wasEnabled) commandSender!!.sendMessage("Debugging is now disabled")
            else commandSender!!.sendMessage("Debugging is already disabled")
        }
    }

    private fun parseEnableDebugCategory(debugCategory: String): Boolean {
        val debugType: DebugType
        try {
            debugType = DebugType.valueOf(debugCategory.uppercase(Locale.getDefault()))
        } catch (ignored: Exception) {
            commandSender!!.sendMessage("Invalid debug type: $debugCategory")
            return false
        }

        LevelledMobs.instance.debugManager.filterDebugTypes.clear()
        LevelledMobs.instance.debugManager.filterDebugTypes.add(debugType)
        commandSender!!.sendMessage("Debug type set to $debugCategory")
        return true
    }

    private fun parseFilter(args: Array<String>) {
        if (args.size == 2) {
            commandSender!!.sendMessage("Please enter a filter option")
            return
        }

        when (args[2].lowercase(Locale.getDefault())) {
            "set-debug" -> parseTypeValues(args, ListTypes.DEBUG)
            "set-entities" -> parseTypeValues(args, ListTypes.ENTITY)
            "set-rules" -> parseTypeValues(args, ListTypes.RULE_NAMES)
            "listen-for" -> updateEvaluationType(args)
            "set-distance-from-players" -> parseNumberValue(args, NumberSettings.MAX_PLAYERS_DIST)
            "set-players" -> parseTypeValues(args, ListTypes.PLAYERS)
            "set-y-height" -> parseYHeight(args)
            "clear-all-filters" -> resetFilters()
        }
    }

    private fun parseYHeight(args: Array<String>) {
        val main = LevelledMobs.instance
        if (args.size <= 3) {
            if (main.debugManager.minYLevel == null && main.debugManager.maxYLevel == null) commandSender!!.sendMessage(
                "Please set a min and/or max y-height, or clear the filter"
            )
            else commandSender!!.sendMessage(
                ("min-y-height: " + main.debugManager.minYLevel) +
                        ", max-y-height: " + main.debugManager.maxYLevel
            )
            return
        }

        when (args[3].lowercase(Locale.getDefault())) {
            "min-y-height" -> parseNumberValue(args, NumberSettings.MIN_Y_LEVEL)
            "max-y-height" -> parseNumberValue(args, NumberSettings.MAX_Y_LEVEL)
            "clear" -> {
                main.debugManager.minYLevel = null
                main.debugManager.maxYLevel = null
                commandSender!!.sendMessage("All y-height filters cleared")
            }

            else -> commandSender!!.sendMessage("Invalid option")
        }
    }

    private fun resetFilters() {
        LevelledMobs.instance.debugManager.resetFilters()
        commandSender!!.sendMessage("All filters have been cleared")
    }

    private fun parseNumberValue(
        args: Array<String>,
        numberSetting: NumberSettings
    ) {
        val argNumber = if (numberSetting == NumberSettings.MAX_PLAYERS_DIST) 3 else 4

        if (args.size == argNumber) {
            commandSender!!.sendMessage("No value was specified")
            return
        }

        val main = LevelledMobs.instance
        val useNull = "none".equals(args[argNumber], ignoreCase = true)
        try {
            val value = if (useNull) null else args[argNumber].toInt()
            when (numberSetting) {
                NumberSettings.MAX_PLAYERS_DIST -> {
                    main.debugManager.maxPlayerDistance = value
                    commandSender!!.sendMessage("Distance from players set to $value")
                }

                NumberSettings.MIN_Y_LEVEL -> {
                    main.debugManager.minYLevel = value
                    commandSender!!.sendMessage("Min y-height set to $value")
                }

                NumberSettings.MAX_Y_LEVEL -> {
                    main.debugManager.maxYLevel = value
                    commandSender!!.sendMessage("Max y-height set to $value")
                }
            }
        } catch (ignored: java.lang.Exception) {
            commandSender!!.sendMessage("Invalid number: " + args[argNumber])
        }
    }

    private fun updateEvaluationType(args: Array<String>) {
        val main = LevelledMobs.instance
        if (args.size == 3) {
            commandSender!!.sendMessage("Current value: " + main.debugManager.listenFor)
            return
        }

        try {
            main.debugManager.listenFor =
                DebugManager.ListenFor.valueOf(args[3].uppercase(Locale.getDefault()))

            when (main.debugManager.listenFor) {
                DebugManager.ListenFor.BOTH -> commandSender!!.sendMessage("Listening for all debug notice events")
                DebugManager.ListenFor.FAILURE -> commandSender!!.sendMessage("Listening for failed debug notice events")
                DebugManager.ListenFor.SUCCESS -> commandSender!!.sendMessage("Listening for successful debug notice events")
            }
        } catch (ignored: java.lang.Exception) {
            commandSender!!.sendMessage("Invalid listen-for type: " + args[3] + ", valid options are: failure, success, both")
        }
    }

    private fun parseTypeValues(
        args: Array<String>,
        listType: ListTypes
    ) {
        if (args.size == 3) {
            viewList(listType)
            return
        }

        when (args[3].lowercase(Locale.getDefault())) {
            "clear" -> {
                clearList(listType)
                val listTypeMsg: String = when (listType) {
                    ListTypes.PLAYERS -> "Players"
                    ListTypes.RULE_NAMES -> "Rule names"
                    ListTypes.ENTITY -> "Entity types"
                    ListTypes.DEBUG -> "Debug types"
                }
                commandSender!!.sendMessage("All filters cleared for $listTypeMsg")
            }

            "add", "remove" -> {
                if (args.size == 4) {
                    commandSender!!.sendMessage("No values were specified for " + args[3])
                    return
                }
                val items = args.toList().subList(4, args.size).toMutableSet()
                addOrRemoveItemsToList("add".equals(args[3], ignoreCase = true), items, listType)
            }

            else -> commandSender!!.sendMessage("Invalid option: " + args[3])
        }
    }

    private fun viewList(listType: ListTypes) {
        val main = LevelledMobs.instance
        val useList: MutableSet<*> = when (listType) {
            ListTypes.DEBUG -> main.debugManager.filterDebugTypes
            ListTypes.ENTITY -> main.debugManager.filterEntityTypes
            ListTypes.RULE_NAMES -> main.debugManager.filterRuleNames
            ListTypes.PLAYERS -> main.debugManager.filterPlayerNames
        }
        val msg = if (useList.isEmpty()) "No values currently defined" else useList.toString()
        commandSender!!.sendMessage(msg)
    }

    private fun clearList(listType: ListTypes) {
        val main = LevelledMobs.instance
        when (listType) {
            ListTypes.DEBUG -> main.debugManager.filterDebugTypes.clear()
            ListTypes.ENTITY -> main.debugManager.filterEntityTypes.clear()
            ListTypes.RULE_NAMES -> main.debugManager.filterRuleNames.clear()
            ListTypes.PLAYERS -> main.debugManager.filterPlayerNames.clear()
        }
    }

    private fun addOrRemoveItemsToList(
        isAdd: Boolean,
        items: MutableSet<String>,
        listType: ListTypes
    ) {
        val main = LevelledMobs.instance
        val dm = main.debugManager
        val optionsAddedOrRemoved = mutableListOf<String>()
        when (listType) {
            ListTypes.DEBUG -> {
                for (debugTypeStr in items) {
                    try {
                        val debugType = DebugType.valueOf(debugTypeStr.uppercase(Locale.getDefault()))
                        if (isAdd) {
                            dm.filterDebugTypes.add(debugType)
                            optionsAddedOrRemoved.add(debugType.name)
                        } else {
                            dm.filterDebugTypes.remove(debugType)
                            optionsAddedOrRemoved.add(debugType.name)
                        }
                    } catch (ignored: java.lang.Exception) {
                        if (isAdd) commandSender!!.sendMessage("Invalid debug type: $debugTypeStr")
                    }
                }
            }

            ListTypes.ENTITY -> {
                for (entityTypeStr in items) {
                    try {
                        val entityType = EntityType.valueOf(entityTypeStr.uppercase(Locale.getDefault()))
                        if (isAdd) {
                            dm.filterEntityTypes.add(entityType)
                            optionsAddedOrRemoved.add(entityType.name)
                        } else {
                            dm.filterEntityTypes.remove(entityType)
                            optionsAddedOrRemoved.add(entityType.name)
                        }
                    } catch (ignored: java.lang.Exception) {
                        if (isAdd) commandSender!!.sendMessage("Invalid entity type: $entityTypeStr")
                    }
                }
            }

            ListTypes.RULE_NAMES -> {
                val allRuleNames: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
                for (ruleInfo in main.rulesParsingManager.getAllRules(false)) {
                    allRuleNames.add(ruleInfo.ruleName.replace(" ", "_"))
                }

                for (ruleName in items) {
                    if (isAdd) {
                        var actualRuleName: String? = null
                        for (foundRuleName in allRuleNames) {
                            if (foundRuleName.equals(ruleName, ignoreCase = true)) {
                                actualRuleName = foundRuleName
                                break
                            }
                        }
                        if (actualRuleName != null) {
                            dm.filterRuleNames.add(actualRuleName.replace(" ", "_"))
                            optionsAddedOrRemoved.add(actualRuleName)
                        } else {
                            commandSender!!.sendMessage("Invalid rule name: $ruleName")
                        }
                    } else {
                        dm.filterRuleNames.remove(ruleName)
                        optionsAddedOrRemoved.add(ruleName)
                    }
                }
            }

            ListTypes.PLAYERS -> {
                // for players we'll allow invalid player names because they might join later
                if (isAdd) {
                    dm.filterPlayerNames.addAll(items)
                    optionsAddedOrRemoved.addAll(items)
                } else {
                    for (playerName in items) {
                        dm.filterPlayerNames.remove(playerName)
                        optionsAddedOrRemoved.add(playerName)
                    }
                }
            }
        }
        if (optionsAddedOrRemoved.isNotEmpty()) {
            val useName: String = listType.name.replace("_", " ").lowercase(Locale.getDefault())
            if (isAdd) commandSender!!.sendMessage("Added values to $useName : $optionsAddedOrRemoved")
            else commandSender!!.sendMessage("Removed values to $useName: $optionsAddedOrRemoved")
        }
    }

    private enum class NumberSettings {
        MAX_PLAYERS_DIST, MIN_Y_LEVEL, MAX_Y_LEVEL
    }

    private enum class ListTypes {
        DEBUG, ENTITY, RULE_NAMES, PLAYERS
    }

    private fun showLEWDebug(sender: CommandSender) {
        if (!sender.hasPermission("levelledmobs.command.debug.lew_debug")) {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
            return
        }

        val result = LivingEntityWrapper.getLEWDebug()
        sender.sendMessage(result)
    }

    private fun clearLEWCache(sender: CommandSender) {
        if (!sender.hasPermission("levelledmobs.command.debug.lew_clear")) {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
            return
        }

        LivingEntityWrapper.clearCache()
        sender.sendMessage("Cleared the LEW cache")
    }

    private fun showSpawnDistance(
        sender: CommandSender,
        args: Array<String>
    ) {
        var player: Player? = null
        if (sender !is Player && args.size < 3) {
            sender.sendMessage("Must specify a player when running this command from console")
            return
        }
        if (args.size >= 3) {
            player = Bukkit.getPlayer(args[2])
            if (player == null) {
                sender.sendMessage("Invalid playername: " + args[2])
                return
            }
        }

        if (player == null) {
            player = sender as Player
        }

        val lmEntity = LevelledMobs.instance.levelledMobsCommand.rulesSubcommand.getMobBeingLookedAt(
            player, true, sender
        )

        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + player.name)
            return
        }

        val distance: Double = lmEntity.distanceFromSpawn

        val locationStr =
            "${lmEntity.livingEntity.location.blockX}, " +
            "${lmEntity.livingEntity.location.blockY}, " +
            "${lmEntity.livingEntity.location.blockZ}"

        val mobLevel = if (lmEntity.isLevelled) lmEntity.getMobLevel.toString() else "0"

        var entityName = lmEntity.typeName
        if (ExternalCompatibilityManager.hasMythicMobsInstalled()
            && ExternalCompatibilityManager.isMythicMob(lmEntity)
        ) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
        }

        val message = String.format(
            "Spawn distance is %s for: %s (lvl %s %s) in %s, %s",
            Utils.round(distance, 1),
            entityName,
            mobLevel,
            lmEntity.nameIfBaby,
            lmEntity.worldName,
            locationStr
        )

        lmEntity.free()
        sender.sendMessage(message)
    }

    private fun showPlayerLocation(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("The command must be run by a player")
            return
        }

        val l = sender.location
        val locationStr =
            "location is ${l.blockX}, ${l.blockY}, ${l.blockZ} in ${l.world.name}"

        sender.sendMessage("Your location: $locationStr")
        Utils.logger.info("Player ${sender.getName()}, location: $locationStr")
    }

    private fun doNbtDump(
        sender: CommandSender,
        args: Array<String>
    ) {
        var player: Player? = null
        if (sender !is Player && args.size < 3) {
            sender.sendMessage("Must specify a player when running this command from console")
            return
        }
        if (args.size >= 3) {
            player = Bukkit.getPlayer(args[2])
            if (player == null) {
                sender.sendMessage("Invalid playername: " + args[2])
                return
            }
        }

        if (player == null) {
            player = sender as Player
        }

        val lmEntity = LevelledMobs.instance.levelledMobsCommand.rulesSubcommand.getMobBeingLookedAt(
            player, true, sender
        )
        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + player.name)
            return
        }

        var entityName = lmEntity.typeName
        if (ExternalCompatibilityManager.hasMythicMobsInstalled()
            && ExternalCompatibilityManager.isMythicMob(lmEntity)
        ) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
        }

        val locationStr =
            "${lmEntity.livingEntity.location.blockX}, " +
            "${lmEntity.livingEntity.location.blockX}, " +
            "${lmEntity.livingEntity.location.blockX}"

        val mobLevel = if (lmEntity.isLevelled) java.lang.String.valueOf(lmEntity.getMobLevel) else "0"

        val message = String.format(
            "Showing nbt dump for: %s (lvl %s %s) in %s, %s\n%s",
            entityName,
            mobLevel,
            lmEntity.nameIfBaby,
            lmEntity.worldName,
            locationStr,
            MiscUtils.getNBTDump(lmEntity.livingEntity)
        )

        lmEntity.free()
        Utils.logger.info(message)
    }

    private fun chunkKillCount(
        sender: CommandSender,
        args: Array<String>
    ) {
        if (args.size >= 3 && "reset".equals(args[2], ignoreCase = true)) {
            LevelledMobs.instance.companion.clearChunkKillCache()
            sender.sendMessage("cache has been cleared")
            return
        }

        showChunkKillCountSyntax(sender)
    }

    private fun showChunkKillCountSyntax(sender: CommandSender) {
        sender.sendMessage("Options: reset")
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String>? {
        if (args.size <= 2) {
            return mutableListOf(
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
            )
        }

        when (args[1].lowercase(Locale.getDefault())) {
            "enable" -> {
                if (args.size == 3) return getDebugTypes()
            }

            "enable-timer" -> {
                if (args.size == 4) return getDebugTypes()
            }

            "chunk-kill-count" -> {
                return mutableListOf("reset")
            }

            "filter-results" -> {
                return parseFilterTabCompletion(args)
            }

            "nbt-dump" -> {
                if (args.size == 3) return null
            }

            "output-debug" -> {
                val values: MutableList<String> = LinkedList()
                for (outputTypes in DebugManager.OutputTypes.entries) {
                    if (LevelledMobs.instance.debugManager.outputType !== outputTypes) values.add(
                        outputTypes.name.replace("_", "-").lowercase(Locale.getDefault())
                    )
                }
                return values
            }
        }
        return mutableListOf()
    }

    private fun getDebugTypes(): MutableList<String> {
        val list = mutableListOf<String>()
        for (debugType in DebugType.entries) {
            list.add(debugType.toString().lowercase(Locale.getDefault()))
        }
        return list
    }

    private fun parseFilterTabCompletion(
        args: Array<String>
    ): MutableList<String> {
        if (args.size == 3) {
            return mutableListOf(
                "clear-all-filters", "set-entities", "set-y-height", "set-distance-from-players",
                "set-players", "listen-for", "set-rules", "set-debug"
            )
        }

        if (args.size == 4 && "set-y-height".equals(args[2], ignoreCase = true)) {
            return mutableListOf("min-y-height", "max-y-height", "clear")
        }

        if (args.size == 4) {
            when (args[2].lowercase(Locale.getDefault())) {
                "set-debug", "set-entities", "set-rules", "set-players" -> {
                    return mutableListOf("add", "remove", "clear")
                }

                "listen-for" -> {
                    val values: MutableList<String> = LinkedList()
                    for (evaluationType in DebugManager.ListenFor.entries) {
                        if (LevelledMobs.instance.debugManager.listenFor !== evaluationType) values.add(
                            evaluationType.name.lowercase(Locale.getDefault())
                        )
                    }
                    return values
                }
            }
        }

        val isAdd = "add".equals(args[3], ignoreCase = true)
        val isRemove = "remove".equals(args[3], ignoreCase = true)

        if (args.size >= 5 && (isAdd || isRemove)) {
            when (args[2].lowercase(Locale.getDefault())) {
                "set-debug" -> { return getUnusedDebugTypes(isAdd) }
                "set-entities" -> { return getUnusedEntityTypes(isAdd) }
                "set-rules" -> { return getUnusedRuleNames(isAdd) }
                "set-players" -> { return getUnusedPlayers(isAdd) }
            }
        }

        return mutableListOf()
    }

    private fun getUnusedDebugTypes(isAdd: Boolean): MutableList<String> {
        val debugs = mutableListOf<String>()
        if (isAdd) {
            for (debugType in DebugType.entries) {
                debugs.add(debugType.toString().lowercase(Locale.getDefault()))
            }
        }

        for (debugType in LevelledMobs.instance.debugManager.filterDebugTypes) {
            if (isAdd) debugs.remove(debugType.toString().lowercase(Locale.getDefault()))
            else debugs.add(debugType.toString().lowercase(Locale.getDefault()))
        }

        return debugs
    }

    private fun getUnusedEntityTypes(
        isAdd: Boolean
    ): MutableList<String> {
        val et = mutableListOf<String>()
        if (isAdd) {
            for (entityType in EntityType.entries) {
                if (LevelledMobs.instance.debugManager.excludedEntityTypes.contains(entityType.name)) continue
                et.add(entityType.toString().lowercase(Locale.getDefault()))
            }
        }

        for (entityType in LevelledMobs.instance.debugManager.filterEntityTypes) {
            if (isAdd) et.remove(entityType.toString().lowercase(Locale.getDefault()))
            else et.add(entityType.toString().lowercase(Locale.getDefault()))
        }

        return et
    }

    private fun getUnusedRuleNames(
        isAdd: Boolean
    ): MutableList<String> {
        val ruleNames: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        if (isAdd) {
            for (ri in LevelledMobs.instance.rulesParsingManager.getAllRules(false)) {
                ruleNames.add(ri.ruleName.replace(" ", "_"))
            }
        }

        for (ruleName in LevelledMobs.instance.debugManager.filterRuleNames) {
            if (isAdd) ruleNames.remove(ruleName)
            else ruleNames.add(ruleName)
        }

        return ArrayList(ruleNames)
    }

    private fun getUnusedPlayers(
        isAdd: Boolean
    ): MutableList<String> {
        val players: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        if (isAdd) {
            for (player in Bukkit.getOnlinePlayers()) {
                players.add(player.name)
            }
        }

        for (playerName in LevelledMobs.instance.debugManager.filterPlayerNames) {
            if (isAdd) players.remove(playerName)
            else players.add(playerName)
        }

        return players.toMutableList()
    }
}