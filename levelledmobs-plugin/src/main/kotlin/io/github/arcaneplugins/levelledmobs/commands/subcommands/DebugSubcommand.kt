package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.papermc.paper.command.brigadier.CommandSourceStack
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
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
import java.util.TreeSet
import java.util.concurrent.CompletableFuture
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
object DebugSubcommand : CommandBase("levelledmobs.command.debug") {
    override val description = "Various commands for debugging."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack> {
        return createLiteralCommand("debug")
            .executes { ctx ->
                ctx.source.sender.sendMessage("Please enter a debug option.")
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("create-zip")
                .then(createStringArgument("confirm")
                    .suggests { _, builder -> builder.suggest("confirm").buildFuture() }
                    .executes { ctx -> createZip(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> createZip(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("chunk-kill-count")
                .then(createStringArgument("reset")
                    .suggests { _, builder -> builder.suggest("reset").buildFuture() }
                    .executes { ctx -> chunkKillCount(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> chunkKillCount(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("view-queues")
                .executes { ctx ->
                    viewQueues(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("nbt-dump")
                .then(createPlayerArgument("target")
                    .executes { ctx -> nbtDump(ctx)
                        return@executes Command.SINGLE_SUCCESS
                    })
                .executes { ctx -> nbtDump(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("my-location")
                .executes { ctx -> showPlayerLocation(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("spawn-distance")
                .then(createPlayerArgument("target")
                    .executes { ctx -> showSpawnDistance(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> showSpawnDistance(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("lew-debug")
                .executes { ctx -> showLEWDebug(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("lew-clear")
                .executes { ctx -> clearLEWCache(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-customdrops")
                .executes { ctx -> showCustomDrops(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-pdc-keys")
                .then(createStringArgument("console")
                    .suggests { _, builder -> builder.suggest("console").buildFuture() }
                    .executes { ctx -> showPDCKeys(ctx, true)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx ->
                    showPDCKeys(ctx, false)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-plugin-definitions")
                .executes { ctx -> showPluginDefinitions(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("enable")
                .then(createStringArgument("category")
                    .suggests { _, builder -> getDebugTypes(builder) }
                    .executes { ctx -> enableOrDisableDebug(ctx, isEnable = true, isEnableAll = false)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> enableOrDisableDebug(ctx, isEnable = true, isEnableAll = false)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("enable-all")
                .executes { ctx -> enableOrDisableDebug(ctx, isEnable = true, isEnableAll = true)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("damage-debug-output")
                .then(createStringArgument("operation")
                    .suggests { _, builder -> builder.suggest("enable").suggest("disable").buildFuture() }
                    .executes { ctx -> processDamageDebugOutput(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> processDamageDebugOutput(ctx)
                    return@executes Command.SINGLE_SUCCESS
                }
            )
            .then(createLiteralCommand("enable-timer")
                .then(createStringArgument("time")
                    .then(createStringArgument("category")
                        .suggests { _, builder -> getDebugTypes(builder) }
                        .executes { ctx -> parseEnableTimer(ctx)
                            return@executes Command.SINGLE_SUCCESS })
                    .executes { ctx -> parseEnableTimer(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> parseEnableTimer(ctx)
                    return@executes Command.SINGLE_SUCCESS
                }
            )
            .then(createLiteralCommand("disable")
                .executes { ctx -> enableOrDisableDebug(ctx, isEnable = false, isEnableAll = false)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createFilterResultsCommand()) // filter-results
            .then(createLiteralCommand("output-debug")
                .then(createStringArgument("output")
                    .suggests { _, builder -> getOutputToTypes(builder) }
                    .executes { ctx -> parseOutputTo(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> parseOutputTo(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("view-debug-status")
                .executes { ctx -> ctx.source.sender.sendMessage(LevelledMobs.instance.debugManager.getDebugStatus())
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("view-queues")
                .executes { ctx -> viewQueues(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .build()
    }

    private fun processDamageDebugOutput(
        ctx: CommandContext<CommandSourceStack>
    ){
        val sender = ctx.source.sender
        val operation = getStringArgument(ctx, "operation", null)
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
        else
            sender.sendMessage("Invalid option: $operation")
    }
//
    private fun createFilterResultsCommand(): LiteralCommandNode<CommandSourceStack>{
        val genericMessage = "Please select a filter option"

        return createLiteralCommand("filter-results")
            .then(buildGenericListTypes("set-debug", ListTypes.DEBUG))
            .executes { ctx -> ctx.source.sender.sendMessage(genericMessage)
                return@executes Command.SINGLE_SUCCESS }
            .then(buildGenericListTypes("set-entities", ListTypes.ENTITY))
            .executes { ctx -> ctx.source.sender.sendMessage(genericMessage)
                return@executes Command.SINGLE_SUCCESS }
            .then(buildGenericListTypes("set-rules", ListTypes.RULE_NAMES))
            .executes { ctx -> ctx.source.sender.sendMessage(genericMessage)
                return@executes Command.SINGLE_SUCCESS }
            .then(buildGenericListTypes("set-players", ListTypes.PLAYERS))
            .executes { ctx -> ctx.source.sender.sendMessage(genericMessage)
                return@executes Command.SINGLE_SUCCESS }
            .then(createLiteralCommand("listen-for")
                .then(createStringArgument("value")
                    .suggests { _, builder -> getListenForValues(builder)  }
                    .executes { ctx -> updateEvaluationType(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> updateEvaluationType(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("set-distance-from-players")
                .then(createStringArgument("value")
                    .executes { ctx -> parseNumberValue(ctx, NumberSettings.MAX_PLAYERS_DIST)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> parseNumberValue(ctx, NumberSettings.MAX_PLAYERS_DIST)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createSetHeightCommands())
            .then(createLiteralCommand("clear-all-filters")
                .executes { ctx -> clearFilters(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS })
            .build()
    }

    private fun createSetHeightCommands() : LiteralCommandNode<CommandSourceStack>{
        return createLiteralCommand("set-y-height")
            .executes { ctx -> showYHeightSettings(ctx.source.sender)
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("min-y-height")
                .then(createStringArgument("value")
                    .executes { ctx -> parseNumberValue(ctx, NumberSettings.MIN_Y_LEVEL)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> showYHeightSettings(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("max-y-height")
                .then(createStringArgument("value")
                    .executes { ctx -> parseNumberValue(ctx, NumberSettings.MAX_Y_LEVEL)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> showYHeightSettings(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("clear")
            .executes { ctx ->
                LevelledMobs.instance.debugManager.minYLevel = null
                LevelledMobs.instance.debugManager.maxYLevel = null
                ctx.source.sender.sendMessage("All y-height filters cleared")
                return@executes Command.SINGLE_SUCCESS
            })
        .build()
    }

    private fun showYHeightSettings(sender: CommandSender){
        sender.sendMessage(
            "min-y-height: ${LevelledMobs.instance.debugManager.minYLevel}" +
            ", max-y-height: ${LevelledMobs.instance.debugManager.maxYLevel}"
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

    private fun getListenForValues(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions>{
        for (evaluationType in DebugManager.ListenFor.entries) {
            if (LevelledMobs.instance.debugManager.listenFor != evaluationType)
                builder.suggest(evaluationType.name.lowercase())
        }

        return builder.buildFuture()
    }

    private fun buildGenericListTypes(
        commandName: String,
        listType: ListTypes
    ): LiteralCommandNode<CommandSourceStack>{
        return createLiteralCommand(commandName)
            .executes { ctx -> parseTypeValues(ctx, listType, OperationType.VIEW)
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("add")
                .executes { ctx -> parseTypeValues(ctx, listType, OperationType.ADD)
                    return@executes Command.SINGLE_SUCCESS
                }
                .then(createGreedyStringArgument("values")
                .suggests { ctx, builder -> getUnusedListTypes(ctx, builder, listType, true) }
                .executes { ctx -> parseTypeValues(ctx, listType, OperationType.ADD)
                    return@executes Command.SINGLE_SUCCESS
                }))
            .then(createLiteralCommand("remove")
                .executes { ctx -> parseTypeValues(ctx, listType, OperationType.REMOVE)
                    return@executes Command.SINGLE_SUCCESS
                }
                .then(createGreedyStringArgument("values")
                    .suggests { ctx, builder -> getUnusedListTypes(ctx, builder, listType, false) }
                    .executes { ctx -> parseTypeValues(ctx, listType, OperationType.REMOVE)
                        return@executes Command.SINGLE_SUCCESS
                    }))
            .then(createLiteralCommand("clear")
                .executes { ctx -> parseTypeValues(ctx, listType, OperationType.CLEAR)
                    return@executes Command.SINGLE_SUCCESS
                })
            .build()
    }

    private fun showPDCKeys(ctx: CommandContext<CommandSourceStack>, showOnConsole: Boolean){
        val sender = ctx.source.sender
        val player = sender as? Player
        if (player == null){
            sender.sendMessage("This command must be run by a player")
            return
        }

        var showOnConsole = showOnConsole
        if (sender is ConsoleCommandSender) showOnConsole = true

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

    private fun createZip(ctx: CommandContext<CommandSourceStack>){
        if (getStringArgumentAsBool(ctx, "confirm"))
            DebugCreator.createDebug(ctx.source.sender)
        else
            MessagesHelper.showMessage(ctx.source.sender, "other.create-debug")
    }

    private fun chunkKillCount(ctx: CommandContext<CommandSourceStack>) {
        val lastNodeName = ctx.nodes.last().node.name
        val name = "reset"

        if (lastNodeName == name && name.equals(ctx.getArgument(name, String::class.java), true)) {
            MainCompanion.instance.clearChunkKillCache()
            ctx.source.sender.sendMessage("cache has been cleared")
        }
        else
            ctx.source.sender.sendMessage("Options: reset")
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
        ctx: CommandContext<CommandSourceStack>
    ) {

        if (!LevelledMobs.instance.ver.isNMSVersionValid) {
            ctx.source.sender.sendMessage("Unable to dump, an unknown NMS version was detected")
            return
        }
        doNbtDump(ctx)
        if (ctx.source.sender !is ConsoleCommandSender) {
            ctx.source.sender.sendMessage("NBT data has been written to the console")
        }
    }

    private fun doNbtDump(ctx: CommandContext<CommandSourceStack>) {
        val optionalTarget = getPlayerArgument(ctx, "target")
        val sender = ctx.source.sender
        val lmEntity = getNearbyMob(sender, optionalTarget) ?: return

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

    private fun parseEnableTimer(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val sender = ctx.source.sender
        val input = getStringArgument(ctx, "time")
        if (input.isEmpty()) {
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

        val disableAfter = Utils.parseTimeUnit(
            input, null, true, sender
        )

        if (disableAfter != null) {
            val category = getStringArgument(ctx, "category")
            if (category.isNotEmpty() && !parseEnableDebugCategory(category, sender))
                return

            main.debugManager.disableAfter = disableAfter
            main.debugManager.disableAfterStr = input
            sender.sendMessage("Debug enabled for $input")
            if (main.debugManager.isEnabled) main.debugManager.timerWasChanged(true)
            else main.debugManager.enableDebug(sender, usetimer = true, bypassFilters = false)
        }
    }

    private fun parseOutputTo(ctx: CommandContext<CommandSourceStack>) {
        val main = LevelledMobs.instance
        val sender = ctx.source.sender
        val output = getStringArgument(ctx, "output")
        if (output.isEmpty()) {
            sender.sendMessage(
                "Current value: " + main.debugManager.outputType.name.lowercase().replace("_", "-")
            )
            return
        }
        var wasInvalid = false

        when (output.lowercase()) {
            "to-console" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CONSOLE
            "to-chat" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_CHAT
            "to-both" -> main.debugManager.outputType = DebugManager.OutputTypes.TO_BOTH
            else -> {
                sender.sendMessage("Invalid option: $output")
                wasInvalid = true
            }
        }
        if (!wasInvalid) sender.sendMessage(
            "Output-debug updated to " + main.debugManager.outputType.name.replace("_", "-")
                .lowercase()
        )

        if (main.debugManager.outputType !== DebugManager.OutputTypes.TO_CONSOLE) {
            sender.sendMessage("WARNING: sending debug messages to chat can cause huge chat spam.")
        }
    }

    private fun showCustomDrops(sender: CommandSender) {
        LevelledMobs.instance.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(sender)
    }

    private fun enableOrDisableDebug(
        ctx: CommandContext<CommandSourceStack>,
        isEnable: Boolean,
        isEnableAll: Boolean
    ){
        val main = LevelledMobs.instance
        val wasEnabled = main.debugManager.isEnabled
        val sender = ctx.source.sender
        val debugCategory = getStringArgument(ctx, "category")

        if (isEnable) {
            val wasTimerEnabled = main.debugManager.isTimerEnabled
            val enableAllChanged = main.debugManager.bypassAllFilters != isEnableAll
            if (debugCategory.isNotEmpty()) {
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
            debugType = DebugType.valueOf(debugCategory.uppercase())
        } catch (_: Exception) {
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
        ctx: CommandContext<CommandSourceStack>,
        numberSetting: NumberSettings
    ) {
        val sender = ctx.source.sender
        val numberStr = getStringArgument(ctx, "value")
        val main = LevelledMobs.instance
        val useNull = numberStr.isEmpty() || "none".equals(numberStr, ignoreCase = true)

        try {
            val value = if (useNull) null else numberStr.toInt()
            when (numberSetting) {
                NumberSettings.MAX_PLAYERS_DIST -> {
                    if (numberStr.isEmpty()){
                        sender.sendMessage("Distance from players current value: " +
                                main.debugManager.maxPlayerDistance)
                    }
                    else{
                        main.debugManager.maxPlayerDistance = value
                        sender.sendMessage("Distance from players set to $value")
                    }
                }

                NumberSettings.MIN_Y_LEVEL -> {
                    if (numberStr.isEmpty()){
                        sender.sendMessage("Min y-height current value: " +
                                main.debugManager.minYLevel)
                    }
                    else{
                        main.debugManager.minYLevel = value
                        sender.sendMessage("Min y-height set to $value")
                    }
                }

                NumberSettings.MAX_Y_LEVEL -> {
                    if (numberStr.isEmpty()){
                        sender.sendMessage("Max y-height current value: " +
                                main.debugManager.maxYLevel)
                    }
                    else{
                        main.debugManager.maxYLevel = value
                        sender.sendMessage("Max y-height set to $value")
                    }
                }
            }
        } catch (_: Exception) {
            sender.sendMessage("Invalid number: $numberStr")
        }
    }

    private fun updateEvaluationType(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val main = LevelledMobs.instance
        val sender = ctx.source.sender
        val value = getStringArgument(ctx, "value")
        if (value.isEmpty()) {
            sender.sendMessage("Current value: " + main.debugManager.listenFor)
            return
        }

        try {
            main.debugManager.listenFor =
                DebugManager.ListenFor.valueOf(value.uppercase())

            when (main.debugManager.listenFor) {
                DebugManager.ListenFor.BOTH -> sender.sendMessage("Listening for all debug notice events")
                DebugManager.ListenFor.FAILURE -> sender.sendMessage("Listening for failed debug notice events")
                DebugManager.ListenFor.SUCCESS -> sender.sendMessage("Listening for successful debug notice events")
            }
        } catch (_: Exception) {
            sender.sendMessage("Invalid listen-for type: $value, valid options are: failure, success, both")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTypeValues(
        ctx: CommandContext<CommandSourceStack>,
        listType: ListTypes,
        operationType: OperationType
    ) {
        val sender = ctx.source.sender
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
                val input = getStringArgument(ctx, "values")
                if (input.isEmpty()){
                    sender.sendMessage("No value was specified")
                    return
                }

                val inputValue = splitStringWithQuotes(input, false)
                addOrRemoveItemsToList(sender, (operationType == OperationType.ADD), inputValue, listType)
            }

            else -> {
                val inputSplit = splitStringWithQuotes(ctx.input, false)
                sender.sendMessage("Invalid option: " + inputSplit.last())
            }
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
        items: MutableList<String>,
        listType: ListTypes
    ) {
        val main = LevelledMobs.instance
        val dm = main.debugManager
        val optionsAddedOrRemoved = mutableListOf<String>()
        when (listType) {
            ListTypes.DEBUG -> {
                for (debugTypeStr in items) {
                    try {
                        val debugType = DebugType.valueOf(debugTypeStr.uppercase())
                        if (isAdd) {
                            dm.filterDebugTypes.add(debugType)
                            optionsAddedOrRemoved.add(debugType.name)
                        } else {
                            dm.filterDebugTypes.remove(debugType)
                            optionsAddedOrRemoved.add(debugType.name)
                        }
                    } catch (_: Exception) {
                        if (isAdd) sender.sendMessage("Invalid debug type: $debugTypeStr")
                    }
                }
            }

            ListTypes.ENTITY -> {
                for (entityTypeStr in items) {
                    try {
                        val entityType = EntityType.valueOf(entityTypeStr.uppercase())
                        if (isAdd) {
                            dm.filterEntityTypes.add(entityType)
                            optionsAddedOrRemoved.add(entityType.name)
                        } else {
                            dm.filterEntityTypes.remove(entityType)
                            optionsAddedOrRemoved.add(entityType.name)
                        }
                    } catch (_: Exception) {
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
            val useName: String = listType.name.replace("_", " ").lowercase()
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
        ctx: CommandContext<CommandSourceStack>
    ) {
        val sender = ctx.source.sender
        val optionalTarget = getPlayerArgument(ctx, "target")
        val lmEntity = getNearbyMob(sender, optionalTarget) ?: return

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

    private fun getNearbyMob(
        sender: CommandSender,
        optionalTarget: Player?
    ): LivingEntityWrapper? {
        if (sender !is Player && optionalTarget == null) {
            sender.sendMessage("Must specify a player when running this command from console")
            return null
        }

        val usePlayer = optionalTarget ?: sender as Player
        val lmEntity = RulesSubcommand.getMobBeingLookedAt(
            usePlayer, true, sender
        )

        if (lmEntity == null) {
            sender.sendMessage("Could not locate any mobs near player: " + usePlayer.name)
            return null
        }

        return lmEntity
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

    private fun getDebugTypes(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        for (debugType in DebugType.entries) {
            builder.suggest(debugType.toString().lowercase())
        }
        return builder.buildFuture()
    }

    private fun getOutputToTypes(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        for (outputType in DebugManager.OutputTypes.entries) {
            if (LevelledMobs.instance.debugManager.outputType != outputType)
                builder.suggest(outputType.name.replace("_", "-").lowercase())
        }
        return builder.buildFuture()
    }

    private fun getUnusedListTypes(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder,
        listType: ListTypes,
        isAdd: Boolean
    ): CompletableFuture<Suggestions> {
        val result: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
        val input = splitStringWithQuotes(ctx.input, false)

        val prefix = StringBuilder()
        val existingItems = mutableListOf<String>()
        // 0  1     2              3         4
        // lm debug filter-results set-debug add apply_multipliers
        for (i in 5..<input.size){
            existingItems.add(input[i].lowercase())
            if (prefix.isNotEmpty()) prefix.append(' ')
            prefix.append(input[i]).append(' ')
        }

        when (listType){
            ListTypes.DEBUG -> {
                if (isAdd) {
                    for (debugType in DebugType.entries) {
                        val debugName = debugType.toString().lowercase()
                        if (!existingItems.contains(debugName))
                            result.add(prefix.toString() + debugName)
                    }
                }

                for (debugType in LevelledMobs.instance.debugManager.filterDebugTypes) {
                    if (isAdd) result.remove(prefix.toString() + debugType.toString().lowercase())
                    else result.add(prefix.toString() + debugType.toString().lowercase())
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
                        result.add(entityType.toString().lowercase())
                    }
                }

                for (entityType in LevelledMobs.instance.debugManager.filterEntityTypes) {
                    if (isAdd) result.remove(entityType.toString().lowercase())
                    else result.add(entityType.toString().lowercase())
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

        result.forEach { value -> builder.suggest(value) }
        return builder.buildFuture()
    }
}