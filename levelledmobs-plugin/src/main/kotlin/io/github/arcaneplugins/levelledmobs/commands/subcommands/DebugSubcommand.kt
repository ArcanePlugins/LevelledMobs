package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.PlayerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.debug.DebugCreator
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.util.MiscUtils
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.Locale
import java.util.TreeSet
import org.bukkit.Bukkit
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
object DebugSubcommand {
    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("debug")
            .withPermission("levelledmobs.command.debug")
            .withShortDescription("Various commands for debugging.")
            .withFullDescription("Various commands for debugging.")
            .executes(CommandExecutor { sender, _ ->
                sender.sendMessage("Please enter a debug option.")
            })
            .withSubcommands(
                CommandAPICommand("create-zip")
                    .withOptionalArguments(StringArgument("confirm"))
                    .executes(CommandExecutor { sender, args -> createZip(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("chunk-kill-count")
                    .withOptionalArguments(StringArgument("reset")
                        .includeSuggestions(ArgumentSuggestions.strings("reset")))
                    .executes(CommandExecutor { sender, args -> chunkKillCount(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("nbt-dump")
                    .withOptionalArguments(PlayerArgument("target")
                        .includeSuggestions(ArgumentSuggestions.strings("target")))
                    .executes(CommandExecutor { sender, args -> nbtDump(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("my-location")
                    .executes(CommandExecutor { sender, _ -> showPlayerLocation(sender) })
            )
            .withSubcommands(
                CommandAPICommand("spawn-distance")
                    .withOptionalArguments(PlayerArgument("target")
                        .includeSuggestions(ArgumentSuggestions.strings("target")))
                    .executes(CommandExecutor { sender, args -> showSpawnDistance(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("lew-debug")
                    .executes(CommandExecutor { sender, _ -> showLEWDebug(sender) })
            )
            .withSubcommands(
                CommandAPICommand("lew-clear")
                    .executes(CommandExecutor { sender, _ -> clearLEWCache(sender) })
            )
            .withSubcommands(
                CommandAPICommand("show-customdrops")
                    .executes(CommandExecutor { sender, _ -> showCustomDrops(sender) })
            )
            .withSubcommands(
                CommandAPICommand("show-pdc-keys")
                    .executes(CommandExecutor { sender, args -> showPDCKeys(sender, args) })
                    .withOptionalArguments(StringArgument("console")
                        .includeSuggestions(ArgumentSuggestions.strings("console")))
            )
            .withSubcommands(
                CommandAPICommand("show-plugin-definitions")
                    .executes(CommandExecutor { sender, _ -> showPluginDefinitions(sender) })
            )
            .withSubcommands(
                CommandAPICommand("enable")
                    .withOptionalArguments(StringArgument("category")
                        .includeSuggestions(ArgumentSuggestions.strings(getDebugTypes())))
                    .executes(CommandExecutor { sender, args ->
                        enableOrDisableDebug(isEnable = true, isEnableAll = false, args.getRaw("category"), sender)
                    })
            )
            .withSubcommands(
                CommandAPICommand("enable-all")
                    .executes(CommandExecutor { sender, _ ->
                        enableOrDisableDebug(isEnable = true, isEnableAll = true, null, sender)
                    })
            )
            .withSubcommands(
                CommandAPICommand("enable-timer")
                    .withArguments(StringArgument("time"))
                    .withOptionalArguments(StringArgument("category")
                        .includeSuggestions(ArgumentSuggestions.strings(getDebugTypes())))
                    .executes(CommandExecutor { sender, args -> parseEnableTimer(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("disable")
                    .executes(CommandExecutor { sender, _ -> enableOrDisableDebug(
                        isEnable = false,
                        isEnableAll = false,
                        debugCategory = null,
                        sender = sender
                    ) })
            )
            .withSubcommands(
                CommandAPICommand("disable-all")
                    .executes(CommandExecutor { sender, _ -> enableOrDisableDebug(
                        isEnable = false,
                        isEnableAll = false,
                        debugCategory = null,
                        sender = sender
                    ) })
            )
            .withSubcommands(
                createFilterResultsCommand() // filter-results
            )
            .withSubcommands(
                CommandAPICommand("output-debug")
                    .withOptionalArguments(StringArgument("output")
                        .includeSuggestions(ArgumentSuggestions.strings{ _ -> getOutputToTypes() }))
                    .executes(CommandExecutor { sender, args -> parseOutputTo(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("view-debug-status")
                    .executes(CommandExecutor { sender, _ ->
                        sender.sendMessage(LevelledMobs.instance.debugManager.getDebugStatus()) })
            )
            .withSubcommands(
                CommandAPICommand("damage-debug-output")
                    .withOptionalArguments(StringArgument("operation")
                        .includeSuggestions(ArgumentSuggestions.strings("enable", "disable")))
                    .executes(CommandExecutor { sender, args -> processDamageDebugOutput(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("view-queues")
                    .executes(CommandExecutor { sender, _ -> viewQueues(sender) })
            )
    }

    private fun processDamageDebugOutput(
        sender: CommandSender,
        args: CommandArguments
    ){
        val operation = args.get("operation") as String?
        val debugMgr = LevelledMobs.instance.debugManager

        if (operation.isNullOrEmpty()){
            val status = if (debugMgr.damageDebugOutputIsEnabled) "enabled" else "disabled"
            sender.sendMessage("Damage Debug Output status: $status")
            return
        }

        if ("enable".equals(operation, true)){
            if (debugMgr.damageDebugOutputIsEnabled)
                sender.sendMessage("Damage Debug Output is enabled.")
            else{
                debugMgr.toggleDamageDebugOutput(true)
                sender.sendMessage("Damage Debug Output is now enabled.")
            }
        }
        else if ("disable".equals(operation, true)){
            if (debugMgr.damageDebugOutputIsEnabled){
                debugMgr.toggleDamageDebugOutput(false)
                sender.sendMessage("Damage Debug Output is now disabled.")
            }
            else
                sender.sendMessage("Damage Debug Output is disabled.")
        }
        else{
            sender.sendMessage("Invalid option: $operation")
        }
    }

    private fun createFilterResultsCommand(): CommandAPICommand{
        return CommandAPICommand("filter-results")
            .executes(CommandExecutor { sender, _ ->
                sender.sendMessage("Please select a filter option") })
            .withSubcommands(
                buildGenericListTypes("set-debug", ListTypes.DEBUG)
            )
            .withSubcommands(
                buildGenericListTypes("set-entities", ListTypes.ENTITY)
            )
            .withSubcommands(
                buildGenericListTypes("set-rules", ListTypes.RULE_NAMES)
            )
            .withSubcommands(
                CommandAPICommand("listen-for")
                    .withOptionalArguments(StringArgument("value")
                        .includeSuggestions(ArgumentSuggestions.strings(getListenForValues())))
                    .executes(CommandExecutor { sender, args -> updateEvaluationType(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("set-distance-from-players")
                    .withOptionalArguments(StringArgument("value"))
                    .executes(CommandExecutor { sender, args ->
                        parseNumberValue(sender, args, NumberSettings.MAX_PLAYERS_DIST) })
            )
            .withSubcommands(
                buildGenericListTypes("set-players", ListTypes.PLAYERS)
            )
            .withSubcommands(
                CommandAPICommand("set-y-height")
                    .executes(CommandExecutor {sender, _ ->
                        sender.sendMessage(
                            "min-y-height: ${LevelledMobs.instance.debugManager.minYLevel}" +
                                ", max-y-height: ${LevelledMobs.instance.debugManager.maxYLevel}"
                        )
                    })
                    .withSubcommands(
                        CommandAPICommand("min-y-height")
                            .withOptionalArguments(StringArgument("value"))
                            .executes(CommandExecutor { sender, args ->
                                parseNumberValue(sender, args, NumberSettings.MIN_Y_LEVEL) })
                    )
                    .withSubcommands(
                        CommandAPICommand("max-y-height")
                            .withOptionalArguments(StringArgument("value"))
                            .executes(CommandExecutor { sender, args ->
                                parseNumberValue(sender, args, NumberSettings.MAX_Y_LEVEL) })
                    )
                    .withSubcommands(
                        CommandAPICommand("clear")
                            .executes(CommandExecutor { sender, _ ->
                                LevelledMobs.instance.debugManager.minYLevel = null
                                LevelledMobs.instance.debugManager.maxYLevel = null
                                sender.sendMessage("All y-height filters cleared")
                            })
                    )
            )
            .withSubcommands(
                CommandAPICommand("clear-all-filters")
                    .executes(CommandExecutor { sender, _ -> clearFilters(sender) })
            )
    }

    private fun viewQueues(sender: CommandSender){
        val nametagQueueNum = LevelledMobs.instance.nametagQueueManager.getNumberQueued()
        val nametagQueueTask = LevelledMobs.instance.nametagQueueManager.queueTask
        val mobQueueNum = LevelledMobs.instance.mobsQueueManager.getNumberQueued()
        val isNametagTaskRunning = if (nametagQueueTask != null) Bukkit.getScheduler().isCurrentlyRunning(nametagQueueTask.taskId) else false
        val nametagtaskStatus = if (nametagQueueTask == null) "(null)" else "id: ${nametagQueueTask.taskId}, is running: $isNametagTaskRunning, is cancelled: ${nametagQueueTask.isCancelled}"
        val mobsTaskStatus = StringBuilder()
        for (task in LevelledMobs.instance.mobsQueueManager.queueTasks.values){
            val isRunning = Bukkit.getScheduler().isCurrentlyRunning(task.taskId)
            mobsTaskStatus.append("\n   ")
            mobsTaskStatus.append("id: ${task.taskId}, is running: $isRunning, is cancelled: ${task.isCancelled}")
        }

        sender.sendMessage("Nametag Manager items: $nametagQueueNum, Mob Queue Manager items: $mobQueueNum\n" +
            "Mobs queue statuses: $mobsTaskStatus\n" +
            "Nametag task status: $nametagtaskStatus")
    }

    private fun getListenForValues(): MutableList<String>{
        val values = mutableListOf<String>()

        for (evaluationType in DebugManager.ListenFor.entries) {
            if (LevelledMobs.instance.debugManager.listenFor != evaluationType)
                values.add(evaluationType.name.lowercase(Locale.getDefault())
            )
        }

        return values
    }

    private fun buildGenericListTypes(
        commandName: String,
        listType: ListTypes
    ): CommandAPICommand{
        return CommandAPICommand(commandName)
            .executes(CommandExecutor { sender, args ->
                parseTypeValues(sender, args, listType, OperationType.VIEW) })
            .withSubcommands(
                CommandAPICommand("add")
                    .withArguments(ListArgumentBuilder<String>("values")
                        .withList {  _ -> getUnusedListTypes(listType, true) }
                        .withStringMapper()
                        .buildGreedy()
                    )
                    .executes(CommandExecutor { sender, args ->
                        parseTypeValues(sender, args, listType, OperationType.ADD) })
            )
            .withSubcommands(
                CommandAPICommand("remove")
                    .withArguments(ListArgumentBuilder<String>("values")
                        .withList {  _ -> getUnusedListTypes(listType, false) }
                        .withStringMapper()
                        .buildGreedy()

                    )
                    .executes(CommandExecutor { sender, args ->
                        parseTypeValues(sender, args, listType, OperationType.REMOVE) })
            )
            .withSubcommands(
                CommandAPICommand("clear")
                    .executes(CommandExecutor { sender, args ->
                        parseTypeValues(sender, args, listType, OperationType.CLEAR) })
            )
    }

    private fun showPDCKeys(sender: CommandSender, args: CommandArguments){
        val player = sender as? Player
        if (player == null){
            sender.sendMessage("This command must be run by a player")
            return
        }

        var showOnConsole = sender is ConsoleCommandSender
        if (args.get("console") as? String != null)
            showOnConsole = true

        val lmEntity = RulesSubcommand.getMobBeingLookedAt(player, true, sender) ?: return
        val results = MiscUtils.getPDCKeys(lmEntity.livingEntity)
        val sb = StringBuilder()
        var isFirst = true

        for (items in results.entries){
            if (isFirst)
                isFirst = false
            else
                sb.append("\n")

            sb.append("key: &b${items.key}&r, ${items.value}")
        }

        val message = if (results.isEmpty()){
            formatDumpMessage(
                "No PDC keys were found for",
                lmEntity,
                null
            )
        } else{
            formatDumpMessage(
                "Showing PDC keys for",
                lmEntity,
                sb.toString()
            )
        }

        lmEntity.free()

        if (showOnConsole && results.isNotEmpty()) {
            Log.inf(message)
            sender.sendMessage("PDC keys have been printed in the console")
        }
        else
            sender.sendMessage(MessageUtils.colorizeAll(message))
    }

    private fun createZip(sender: CommandSender, args: CommandArguments){
        if ("confirm".equals(args.get("confirm") as? String, ignoreCase = true)) {
            DebugCreator.createDebug(sender)
        } else {
            MessagesHelper.showMessage(sender, "other.create-debug")
        }
    }

    private fun chunkKillCount(sender: CommandSender, args: CommandArguments){
        if ("reset".equals(args.get("reset") as? String, ignoreCase = true)) {
            MainCompanion.instance.clearChunkKillCache()
            sender.sendMessage("cache has been cleared")
            return
        }

        sender.sendMessage("Options: reset")
    }

    private fun showPluginDefinitions(sender: CommandSender){
        val ext = ExternalCompatibilityManager.instance
        if (ext.externalPluginDefinitions.isEmpty()){
            sender.sendMessage("No external plugins defined")
        }

        val sb = StringBuilder("Currently defined plugins:")
        for (plugin in ext.externalPluginDefinitions.values){
            sb.append("\n    ")
            sb.append(plugin.toString())
        }

        sender.sendMessage(sb.toString())
    }

    private fun nbtDump(
        sender: CommandSender,
        args: CommandArguments
    ) {

        if (!LevelledMobs.instance.ver.isNMSVersionValid) {
            sender.sendMessage("Unable to dump, an unknown NMS version was detected")
            return
        }
        doNbtDump(sender, args)
        if (sender !is ConsoleCommandSender) {
            sender.sendMessage("NBT data has been written to the console")
        }
    }

    private fun doNbtDump(
        sender: CommandSender,
        args: CommandArguments
    ) {
        val optionalTarget = args.get("target") as? Player
        if (sender !is Player && optionalTarget == null) {
            sender.sendMessage("Must specify a player when running this command from console")
            return
        }

        val usePlayer = optionalTarget ?: sender as Player
        val lmEntity = RulesSubcommand.getMobBeingLookedAt(
            usePlayer, true, sender
        )
        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + usePlayer.name)
            return
        }

        val message = formatDumpMessage(
            "Showing nbt dump for",
            lmEntity,
            MiscUtils.getNBTDump(lmEntity.livingEntity)
        )

        lmEntity.free()
        Log.inf(message)
    }

    @Suppress("DEPRECATION")
    private fun formatDumpMessage(
        messageStart: String,
        lmEntity: LivingEntityWrapper,
        values: String?
    ): String {
        var entityName = lmEntity.nameIfBaby
        if (ExternalCompatibilityManager.hasMythicMobsInstalled
            && ExternalCompatibilityManager.isMythicMob(lmEntity)
        ) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
        }
        else if (lmEntity.livingEntity.customName != null)
            entityName = lmEntity.livingEntity.customName!!

        val locationStr =
            "${lmEntity.livingEntity.location.blockX}, " +
                    "${lmEntity.livingEntity.location.blockY}, " +
                    "${lmEntity.livingEntity.location.blockZ}"

        val mobLevel = if (lmEntity.isLevelled) lmEntity.getMobLevel.toString() else "0"
        val showValues = if (values != null) "\n$values" else ""

        return if (lmEntity.nameIfBaby.equals(entityName, ignoreCase = true))
            "$messageStart: $entityName (lvl $mobLevel) in ${lmEntity.worldName}, $locationStr&r$showValues"
        else
            "$messageStart: $entityName (lvl $mobLevel ${lmEntity.typeName}) in ${lmEntity.worldName}, $locationStr&r$showValues"
    }

    private fun parseEnableTimer(sender: CommandSender, args: CommandArguments) {
        val input = args.get("time") as? String
        if (input == null) {
            sender.sendMessage("No value was specified")
            return
        }

        val main = LevelledMobs.instance
        if ("0" == input || "none".equals(input, ignoreCase = true)) {
            main.debugManager.disableAfter = null
            main.debugManager.disableAfterStr = null
            main.debugManager.timerWasChanged(false)
            sender.sendMessage("Debug timer disabled")
            return
        }

        var disableAfter = Utils.parseTimeUnit(
            input, null, true, sender
        )

        if (disableAfter != null) {
            val category = args.get("category") as? String
            if (category != null) {
                if (!parseEnableDebugCategory(category, sender)) return
            }
            main.debugManager.disableAfter = disableAfter
            main.debugManager.disableAfterStr = input
            sender.sendMessage("Debug enabled for $input")
            if (main.debugManager.isEnabled) main.debugManager.timerWasChanged(true)
            else main.debugManager.enableDebug(sender, usetimer = true, bypassFilters = false)
        }
    }

    private fun parseOutputTo(sender: CommandSender, args: CommandArguments) {
        val main = LevelledMobs.instance
        val output = args.get("output") as? String
        if (output == null) {
            sender.sendMessage(
                "Current value: " + main.debugManager.outputType.name.lowercase().replace("_", "-")
            )
            return
        }
        var wasInvalid = false

        when (output.lowercase(Locale.getDefault())) {
            "to-console" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CONSOLE
            "to-chat" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CHAT
            "to-both" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_BOTH
            else -> {
                sender.sendMessage("Invalid option: " + args[2])
                wasInvalid = true
            }
        }
        if (!wasInvalid) sender.sendMessage(
            "Output-debug updated to " + main.debugManager.outputType.name.replace("_", "-")
                .lowercase(Locale.getDefault())
        )

        if (main.debugManager.outputType !== DebugManager.OutputTypes.TO_CONSOLE) {
            sender.sendMessage("WARNING: sending debug messages to chat can cause huge chat spam.")
        }
    }

    private fun showCustomDrops(sender: CommandSender) {
        LevelledMobs.instance.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(sender)
    }

    private fun enableOrDisableDebug(
        isEnable: Boolean,
        isEnableAll: Boolean,
        debugCategory: String?,
        sender: CommandSender
    ){
        val main = LevelledMobs.instance
        val wasEnabled = main.debugManager.isEnabled

        if (isEnable) {
            val wasTimerEnabled = main.debugManager.isTimerEnabled
            val enableAllChanged = main.debugManager.bypassAllFilters != isEnableAll
            if (debugCategory != null) {
                if (!parseEnableDebugCategory(debugCategory, sender)) return
            }
            main.debugManager.enableDebug(sender, false, isEnableAll)
            if (wasEnabled && !enableAllChanged) {
                if (wasTimerEnabled) sender.sendMessage("Debugging is already enabled, disabled timer")
                else sender.sendMessage("Debugging is already enabled")
            } else {
                if (isEnableAll) sender.sendMessage("All debug options enabled")
                else sender.sendMessage("Debugging is now enabled")
            }
        } else {
            main.debugManager.disableDebug()
            if (wasEnabled) sender.sendMessage("Debugging is now disabled")
            else sender.sendMessage("Debugging is already disabled")
        }
    }

    private fun parseEnableDebugCategory(
        debugCategory: String,
        sender: CommandSender
    ): Boolean {
        val debugType: DebugType
        try {
            debugType = DebugType.valueOf(debugCategory.uppercase(Locale.getDefault()))
        } catch (ignored: Exception) {
            sender.sendMessage("Invalid debug type: $debugCategory")
            return false
        }

        LevelledMobs.instance.debugManager.filterDebugTypes.clear()
        LevelledMobs.instance.debugManager.filterDebugTypes.add(debugType)
        sender.sendMessage("Debug type set to $debugCategory")
        return true
    }

    private fun clearFilters(sender: CommandSender) {
        LevelledMobs.instance.debugManager.resetFilters()
        sender.sendMessage("All filters have been cleared")
    }

    private fun parseNumberValue(
        sender: CommandSender,
        args: CommandArguments,
        numberSetting: NumberSettings
    ) {
        val numberStr = args.get("value") as? String
        val main = LevelledMobs.instance
        val useNull = numberStr == null || "none".equals(numberStr, ignoreCase = true)

        try {
            val value = if (useNull) null else numberStr!!.toInt()
            when (numberSetting) {
                NumberSettings.MAX_PLAYERS_DIST -> {
                    if (numberStr == null){
                        sender.sendMessage("Distance from players current value: " +
                                main.debugManager.maxPlayerDistance)
                    }
                    else{
                        main.debugManager.maxPlayerDistance = value
                        sender.sendMessage("Distance from players set to $value")
                    }
                }

                NumberSettings.MIN_Y_LEVEL -> {
                    if (numberStr == null){
                        sender.sendMessage("Min y-height current value: " +
                                main.debugManager.minYLevel)
                    }
                    else{
                        main.debugManager.minYLevel = value
                        sender.sendMessage("Min y-height set to $value")
                    }
                }

                NumberSettings.MAX_Y_LEVEL -> {
                    if (numberStr == null){
                        sender.sendMessage("Max y-height current value: " +
                                main.debugManager.maxYLevel)
                    }
                    else{
                        main.debugManager.maxYLevel = value
                        sender.sendMessage("Max y-height set to $value")
                    }
                }
            }
        } catch (ignored: Exception) {
            sender.sendMessage("Invalid number: $numberStr")
        }
    }

    private fun updateEvaluationType(
        sender: CommandSender,
        args: CommandArguments
    ) {
        val main = LevelledMobs.instance
        val value = args.get("value") as? String
        if (value == null) {
            sender.sendMessage("Current value: " + main.debugManager.listenFor)
            return
        }

        try {
            main.debugManager.listenFor =
                DebugManager.ListenFor.valueOf(value.uppercase(Locale.getDefault()))

            when (main.debugManager.listenFor) {
                DebugManager.ListenFor.BOTH -> sender.sendMessage("Listening for all debug notice events")
                DebugManager.ListenFor.FAILURE -> sender.sendMessage("Listening for failed debug notice events")
                DebugManager.ListenFor.SUCCESS -> sender.sendMessage("Listening for successful debug notice events")
            }
        } catch (ignored: Exception) {
            sender.sendMessage("Invalid listen-for type: $value, valid options are: failure, success, both")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTypeValues(
        sender: CommandSender,
        args: CommandArguments,
        listType: ListTypes,
        operationType: OperationType
    ) {
        if (operationType == OperationType.VIEW) {
            viewList(sender, listType)
            return
        }

        when (operationType) {
            OperationType.CLEAR -> {
                clearList(listType)
                val listTypeMsg: String = when (listType) {
                    ListTypes.PLAYERS -> "Players"
                    ListTypes.RULE_NAMES -> "Rule names"
                    ListTypes.ENTITY -> "Entity types"
                    ListTypes.DEBUG -> "Debug types"
                }
                sender.sendMessage("All filters cleared for $listTypeMsg")
            }

            OperationType.ADD, OperationType.REMOVE -> {
                val input = args.get("values")
                if (input == null){
                    sender.sendMessage("No value was specified")
                    return
                }

                val inputValue = (input as java.util.ArrayList<String>).toMutableSet()
                addOrRemoveItemsToList(sender, (operationType == OperationType.ADD), inputValue, listType)
            }

            else -> sender.sendMessage("Invalid option: " + args[3])
        }
    }

    private fun viewList(sender: CommandSender, listType: ListTypes) {
        val main = LevelledMobs.instance
        val useList: MutableSet<*> = when (listType) {
            ListTypes.DEBUG -> main.debugManager.filterDebugTypes
            ListTypes.ENTITY -> main.debugManager.filterEntityTypes
            ListTypes.RULE_NAMES -> main.debugManager.filterRuleNames
            ListTypes.PLAYERS -> main.debugManager.filterPlayerNames
        }
        val msg = if (useList.isEmpty()) "No values currently defined" else useList.toString()
        sender.sendMessage(msg)
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
        sender: CommandSender,
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
                    } catch (ignored: Exception) {
                        if (isAdd) sender.sendMessage("Invalid debug type: $debugTypeStr")
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
                    } catch (ignored: Exception) {
                        if (isAdd) sender.sendMessage("Invalid entity type: $entityTypeStr")
                    }
                }
            }

            ListTypes.RULE_NAMES -> {
                val allRuleNames: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
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
                            sender.sendMessage("Invalid rule name: $ruleName")
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
            if (isAdd) sender.sendMessage("Added values to $useName : $optionsAddedOrRemoved")
            else sender.sendMessage("Removed values from $useName: $optionsAddedOrRemoved")
        }
    }

    private enum class OperationType{
        ADD, REMOVE, CLEAR, VIEW
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
        args: CommandArguments
    ) {
        val optionalTarget = args.get("target") as? Player
        if (sender !is Player && optionalTarget == null) {
            sender.sendMessage("Must specify a player when running this command from console")
            return
        }

        val usePlayer = optionalTarget ?: sender as Player
        val lmEntity = RulesSubcommand.getMobBeingLookedAt(
            usePlayer, true, sender
        )

        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + usePlayer.name)
            return
        }

        val distance: Double = lmEntity.distanceFromSpawn

        val locationStr =
            "${lmEntity.livingEntity.location.blockX}, " +
            "${lmEntity.livingEntity.location.blockY}, " +
            "${lmEntity.livingEntity.location.blockZ}"

        val mobLevel = if (lmEntity.isLevelled) lmEntity.getMobLevel.toString() else "0"

        var entityName = lmEntity.typeName
        if (ExternalCompatibilityManager.hasMythicMobsInstalled
            && ExternalCompatibilityManager.isMythicMob(lmEntity)
        ) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
        }

        val message =
            "Spawn distance is ${Utils.round(distance, 1)} for: $entityName " +
                    "(lvl $mobLevel ${lmEntity.nameIfBaby}) in ${lmEntity.worldName}, $locationStr"

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
    }

    private fun getDebugTypes(): MutableList<String> {
        val list = mutableListOf<String>()
        for (debugType in DebugType.entries) {
            list.add(debugType.toString().lowercase(Locale.getDefault()))
        }
        return list
    }

    private fun getOutputToTypes(): Array<String>{
        val values = mutableListOf<String>()
        for (outputType in DebugManager.OutputTypes.entries) {
            if (LevelledMobs.instance.debugManager.outputType != outputType)
                values.add(outputType.name.replace("_", "-").lowercase())
        }
        return values.toTypedArray()
    }

    private fun getUnusedListTypes(
        listType: ListTypes,
        isAdd: Boolean
    ): MutableList<String>{
        val result: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)

        when (listType){
            ListTypes.DEBUG -> {
                if (isAdd) {
                    for (debugType in DebugType.entries) {
                        result.add(debugType.toString().lowercase(Locale.getDefault()))
                    }
                }

                for (debugType in LevelledMobs.instance.debugManager.filterDebugTypes) {
                    if (isAdd) result.remove(debugType.toString().lowercase(Locale.getDefault()))
                    else result.add(debugType.toString().lowercase(Locale.getDefault()))
                }
            }
            ListTypes.PLAYERS -> {
                if (isAdd) {
                    for (player in Bukkit.getOnlinePlayers()) {
                        result.add(player.name)
                    }
                }

                for (playerName in LevelledMobs.instance.debugManager.filterPlayerNames) {
                    if (isAdd) result.remove(playerName)
                    else result.add(playerName)
                }
            }
            ListTypes.ENTITY -> {
                if (isAdd) {
                    for (entityType in EntityType.entries) {
                        if (LevelledMobs.instance.levelManager.forcedBlockedEntityTypes.contains(entityType)) continue
                        result.add(entityType.toString().lowercase(Locale.getDefault()))
                    }
                }

                for (entityType in LevelledMobs.instance.debugManager.filterEntityTypes) {
                    if (isAdd) result.remove(entityType.toString().lowercase(Locale.getDefault()))
                    else result.add(entityType.toString().lowercase(Locale.getDefault()))
                }
            }
            ListTypes.RULE_NAMES -> {
                if (isAdd) {
                    for (ri in LevelledMobs.instance.rulesParsingManager.getAllRules(false)) {
                        result.add(ri.ruleName.replace(" ", "_"))
                    }
                }

                for (ruleName in LevelledMobs.instance.debugManager.filterRuleNames) {
                    if (isAdd) result.remove(ruleName)
                    else result.add(ruleName)
                }
            }
        }

        return result.toMutableList()
    }
}