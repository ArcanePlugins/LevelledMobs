package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import java.util.Locale
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Gives the user a specialized spawner that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
object SpawnerSubcommand : SpawnerBaseClass() {
    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("spawner")
            .withPermission("levelledmobs.command.spawner")
            .withShortDescription("Various commands for creating spawner cubes.")
            .withFullDescription("Various commands for creating spawner cubes.")
            .executes(CommandExecutor { sender, _ ->
                MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.usage")
            })
            .withSubcommands(
                CommandAPICommand("create")
                .executes(CommandExecutor { sender, args ->
                    processResults(sender, args, OperationEnum.CREATE)
                })
                .withOptionalArguments(
                    ListArgumentBuilder<String>("values")
                        .allowAnyValue(true)
                        .allowDuplicates(true)
                        .withList { info -> buildTabSuggestions(allSpawnerOptions, info) }
                        .withStringMapper()
                        .buildGreedy()
                )
            )
            .withSubcommands(
                CommandAPICommand("copy")
                    .executes(CommandExecutor { sender, args ->
                        if (sender !is Player)
                            MessagesHelper.showMessage(sender, "common.players-only")
                        else
                            processResults(sender, args, OperationEnum.COPY)
                    })
                    .withOptionalArguments(StringArgument("value")
                        .replaceSuggestions(ArgumentSuggestions.strings("on", "off"))
                    )
            )
            .withSubcommands(
                CommandAPICommand("info")
                    .executes(CommandExecutor { sender, args ->
                        if (sender !is Player)
                            MessagesHelper.showMessage(sender, "common.players-only")
                        else
                            processResults(sender, args, OperationEnum.INFO)
                    })
                    .withOptionalArguments(StringArgument("value")
                        .replaceSuggestions(ArgumentSuggestions.strings("on", "off"))
                    )
            )
    }

    private fun processResults(
        sender: CommandSender,
        input: CommandArguments,
        operation: OperationEnum
    ) {
        if (input.rawArgs.isEmpty()){
            when (operation){
                OperationEnum.CREATE -> {
                    MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.usage")
                }
                OperationEnum.COPY -> {
                    val playerId = (sender as Player).uniqueId
                    MessagesHelper.showMessage(sender,
                        if (MainCompanion.instance.spawnerCopyIds.contains(playerId))
                            "command.levelledmobs.spawner.copy.status-enabled"
                        else
                            "command.levelledmobs.spawner.copy.status-not-enabled"
                    )
                }
                OperationEnum.INFO -> {
                    val playerId = (sender as Player).uniqueId
                    MessagesHelper.showMessage(sender,
                        if (MainCompanion.instance.spawnerInfoIds.contains(playerId))
                            "command.levelledmobs.spawner.info.status-enabled"
                        else
                            "command.levelledmobs.spawner.info.status-not-enabled"
                    )
                }
            }
            return
        }

        val args = Utils.splitStringWithQuotes(input.rawArgs[0])
        var hasGivePlayer = false
        for (i in 0 until args.size) {
            if ("/giveplayer".equals(args[i], ignoreCase = true)) {
                hasGivePlayer = true
                break
            }
        }

        if ((!hasGivePlayer || operation != OperationEnum.CREATE)
            && sender !is Player
        ) {
            val messageName =
                if (operation != OperationEnum.CREATE)
                    "common.players-only" else "command.levelledmobs.spawner.no-player"

            MessagesHelper.showMessage(sender, messageName)
            return
        }

        when (operation) {
            OperationEnum.CREATE -> parseCreateCommand(sender, args)
            OperationEnum.COPY -> parseCopyCommand(sender, input)
            OperationEnum.INFO -> parseInfoCommand(sender, input)
        }
    }

    private fun parseInfoCommand(
        sender: CommandSender,
        input: CommandArguments
    ) {
        val playerId = (sender as Player).uniqueId
        val main = LevelledMobs.instance
        val value = input.get("value") as String

        if ("on".equals(value, ignoreCase = true)) {
            if (main.mainCompanion.spawnerCopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(sender, playerId)
            }

            main.mainCompanion.spawnerInfoIds.add(playerId)
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.info.enabled")
        } else if ("off".equals(value, ignoreCase = true)) {
            infoGotDisabled(sender, playerId)
        }
    }

    private val allSpawnerOptions = mutableListOf(
        "/name", "/customdropid", "/spawntype", "/giveplayer", "/lore", "/minlevel", "/maxlevel",
        "/delay",
        "/maxnearbyentities", "/minspawndelay", "/maxspawndelay", "/requiredplayerrange",
        "/spawncount", "/spawnrange", "/nolore"
    )

    private fun parseCopyCommand(
        sender: CommandSender,
        input: CommandArguments
    ) {
        val main = LevelledMobs.instance
        val playerId = (sender as Player).uniqueId
        val value = input.get("value") as String

        if ("on".equals(value, ignoreCase = true)) {
            if (main.mainCompanion.spawnerInfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(sender, playerId)
            }

            main.mainCompanion.spawnerCopyIds.add(playerId)
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.copy.enabled")
        } else if ("off".equals(value, ignoreCase = true)) {
            copyGotDisabled(sender, playerId)
        }
    }

    private fun copyGotDisabled(sender: CommandSender, playerId: UUID) {
        MainCompanion.instance.spawnerCopyIds.remove(playerId)
        MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.copy.disabled")
    }

    private fun infoGotDisabled(sender: CommandSender, playerId: UUID) {
        MainCompanion.instance.spawnerInfoIds.remove(playerId)
        MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.info.disabled")
    }

    private fun parseCreateCommand(
        sender: CommandSender,
        args: MutableList<String>
    ) {
        hadInvalidArg = false

        val info = CustomSpawnerInfo()
        if (sender is Player) {
            info.player = sender
        }

        // arguments with no values go here:
        for (i in 1 until args.size) {
            val arg = args[i]
            if ("/nolore".equals(arg, ignoreCase = true)) {
                info.noLore = true
                break
            }
        }

        for (i in 0 until allSpawnerOptions.size - 1) {
            val mustBeANumber = (i > 4)
            val command = allSpawnerOptions[i]
            val foundValue = getArgValue(command, args, mustBeANumber)
            if (hadInvalidArg) {
                return
            }
            if (foundValue.isNullOrEmpty()) {
                continue
            }

            when (command) {
                "/name" -> info.customName = foundValue
                "/customdropid" -> info.customDropId = foundValue
                "/lore" -> info.customLore = foundValue
                "/spawntype" -> {
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.uppercase(Locale.getDefault()))
                    } catch (ignored: Exception) {
                        sender.sendMessage("Invalid spawn type: $foundValue")
                        return
                    }
                }

                "/minlevel" -> info.minLevel = foundValue.toInt()
                "/maxlevel" -> info.maxLevel = foundValue.toInt()
                "/delay" -> info.delay = foundValue.toInt()
                "/maxnearbyentities" -> info.maxNearbyEntities = foundValue.toInt()
                "/minspawndelay" -> info.minSpawnDelay = foundValue.toInt()
                "/maxspawndelay" -> info.maxSpawnDelay = foundValue.toInt()
                "/requiredplayerrange" -> info.requiredPlayerRange = foundValue.toInt()
                "/spawncount" -> info.spawnCount = foundValue.toInt()
                "/spawnrange" -> info.spawnRange = foundValue.toInt()
                "/giveplayer" -> {
                    if (foundValue.isEmpty()) {
                        MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.no-player-specified")
                        return
                    }
                    try {
                        info.player = Bukkit.getPlayer(foundValue)
                    } catch (e: Exception) {
                        MessagesHelper.showMessage(sender, "common.player-offline", "%player%", foundValue)
                        return
                    }
                    if (info.player == null) {
                        MessagesHelper.showMessage(sender, "common.player-offline", "%player%", foundValue)
                        return
                    }
                }
            }
        }

        if (info.minLevel == -1 && info.maxLevel == -1) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.no-level-specified")
            return
        }

        if (info.player == null) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.no-player-specified")
            return
        }

        generateSpawner(sender, info)
    }

    fun generateSpawner(
        sender: CommandSender,
        info: CustomSpawnerInfo
    ) {
        if (info.customName != null) {
            info.customName = colorizeAll(info.customName)
        }

        val item = ItemStack(Material.SPAWNER)
        val meta = item.itemMeta
        if (meta != null) {
            setMetaItems(meta, info, "LM Spawner")

            meta.persistentDataContainer
                .set(NamespacedKeys.keySpawner, PersistentDataType.INTEGER, 1)
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER,
                    info.minLevel
                )
            meta.persistentDataContainer
                .set(
                    NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER,
                    info.maxLevel
                )
            if (!info.customDropId.isNullOrEmpty()) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerCustomDropId,
                        PersistentDataType.STRING, info.customDropId!!
                    )
            }
            if (info.spawnType != EntityType.UNKNOWN) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING,
                        info.spawnType.toString()
                    )
            }
            if (info.spawnRange != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerSpawnRange,
                        PersistentDataType.INTEGER, info.spawnRange!!
                    )
            }
            if (info.minSpawnDelay != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerMinSpawnDelay,
                        PersistentDataType.INTEGER, info.minSpawnDelay!!
                    )
            }
            if (info.maxSpawnDelay != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerMaxSpawnDelay,
                        PersistentDataType.INTEGER, info.maxSpawnDelay!!
                    )
            }
            if (info.spawnCount != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerSpawnCount,
                        PersistentDataType.INTEGER, info.spawnCount!!
                    )
            }
            if (info.requiredPlayerRange != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerRequiredPlayerRange,
                        PersistentDataType.INTEGER, info.requiredPlayerRange!!
                    )
            }
            if (info.maxNearbyEntities != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerMaxNearbyEntities,
                        PersistentDataType.INTEGER, info.maxNearbyEntities!!
                    )
            }
            if (info.delay != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerDelay, PersistentDataType.INTEGER,
                        info.delay!!
                    )
            }
            if (info.customName != null) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING,
                        info.customName!!
                    )
            }

            item.setItemMeta(meta)
        }

        var useInvSlotNum = info.player!!.inventory.heldItemSlot
        if (info.player!!.inventory.getItem(useInvSlotNum) != null) {
            useInvSlotNum = -1
        }

        if (useInvSlotNum == -1) {
            for (i in 0..35) {
                if (info.player!!.inventory.getItem(i) == null) {
                    useInvSlotNum = i
                    break
                }
            }
        }

        if (useInvSlotNum == -1) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.inventory-full", info.player!!.name, "")
            return
        }

        info.player!!.inventory.setItem(useInvSlotNum, item)
        val playerName: String = if (LevelledMobs.instance.ver.isRunningPaper) PaperUtils.getPlayerDisplayName(info.player)
        else SpigotUtils.getPlayerDisplayName(info.player)

        val message = getMessage(
            "command.levelledmobs.spawner.spawner-give-message-console",
            arrayOf("%minlevel%", "%maxlevel%", "%playername%"),
            arrayOf(info.minLevel.toString(), info.maxLevel.toString(), playerName)
        )

        if (message.isNotEmpty()) {
            Log.inf(message[0].replace(LevelledMobs.instance.configUtils.prefix + " ", ""))
        }

        MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.spawner-give-message", info.player!!.name, "")
    }

//    override fun parseTabCompletions(
//        sender: CommandSender,
//        args: Array<String>
//    ): MutableList<String> {
//        if (!sender.hasPermission("levelledmobs.command.spawner")) {
//            return mutableListOf()
//        }
//
//        if (args.size <= 2) {
//            return mutableListOf("copy", "create", "info")
//        }
//
//        if ("create".equals(args[1], ignoreCase = true)) {
//            return tabCompletionsCreate(args)
//        } else if (("info".equals(args[1], ignoreCase = true) || "copy".equals(args[1], ignoreCase = true))
//            && args.size == 3
//        ) {
//            return mutableListOf("on", "off")
//        }
//
//        return mutableListOf()
//    }
//
//    private fun tabCompletionsCreate(
//        args: Array<String>
//    ): MutableList<String> {
//        if (args[args.size - 2].isNotEmpty()) {
//            when (args[args.size - 2].lowercase(Locale.getDefault())) {
//                "/spawntype" -> {
//                    val entityNames: MutableList<String> = LinkedList()
//                    for (entityType in EntityType.entries) {
//                        entityNames.add(entityType.toString().lowercase(Locale.getDefault()))
//                    }
//                    return entityNames
//                }
//
//                "/delay" -> {
//                    return mutableListOf("0")
//                }
//
//                "/minspawndelay" -> {
//                    return mutableListOf("200")
//                }
//
//                "/maxspawndelay" -> {
//                    return mutableListOf("800")
//                }
//
//                "/maxnearbyentities", "/requiredplayerrange" -> {
//                    return mutableListOf("16")
//                }
//
//                "/spawncount", "/spawnrange" -> {
//                    return mutableListOf("4")
//                }
//
//                "/giveplayer" -> {
//                    val players: MutableList<String> = LinkedList()
//                    for (player in Bukkit.getOnlinePlayers()) {
//                        players.add(player.name)
//                    }
//                    players.sortWith(String.CASE_INSENSITIVE_ORDER)
//                    return players
//                }
//            }
//        }
//
//        //return checkTabCompletion(allSpawnerOptions, args)
//        return mutableListOf()
//    }

    private enum class OperationEnum {
        CREATE,
        COPY,
        INFO
    }
}