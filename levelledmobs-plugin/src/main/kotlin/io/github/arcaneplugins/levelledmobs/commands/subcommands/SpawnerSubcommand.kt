package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.util.LinkedList
import java.util.Locale
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
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
class SpawnerSubcommand : SpawnerBaseClass(), Subcommand {
    init {
        startingArgNum = 2
    }

    private val allSpawnerOptions = mutableListOf(
        "/name", "/customdropid", "/spawntype", "/giveplayer", "/lore", "/minlevel", "/maxlevel",
        "/delay",
        "/maxnearbyentities", "/minspawndelay", "/maxspawndelay", "/requiredplayerrange",
        "/spawncount", "/spawnrange", "/nolore"
    )

    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        commandSender = sender
        messageLabel = label

        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
            return
        }

        if (args.size < 2) {
            showMessage("command.levelledmobs.spawner.usage")
            return
        }

        val operationEnum: OperationEnum =
            when (args[1].lowercase(Locale.getDefault())) {
                "copy" -> OperationEnum.COPY
                "info" -> OperationEnum.INFO
                else -> OperationEnum.CREATE
            }

        var hasGivePlayer = false
        for (i in 2 until args.size) {
            if ("/giveplayer".equals(args[i], ignoreCase = true)) {
                hasGivePlayer = true
                break
            }
        }

        if ((!hasGivePlayer || operationEnum != OperationEnum.CREATE)
            && sender !is Player
        ) {
            val messageName =
                if (operationEnum != OperationEnum.CREATE)
                    "common.players-only" else "command.levelledmobs.spawner.no-player"

            showMessage(messageName)
            return
        }

        when (args[1].lowercase(Locale.getDefault())) {
            "create" -> parseCreateCommand(args)
            "copy" -> parseCopyCommand(args)
            "info" -> parseInfoCommand(args)
        }
    }

    private fun parseInfoCommand(
        args: Array<String>
    ) {
        val playerId = (commandSender as Player).uniqueId
        val main = LevelledMobs.instance

        if (args.size == 2) {
            showMessage(
                if (main.companion.spawnerInfoIds.contains(playerId)) "command.levelledmobs.spawner.info.status-enabled"
                else "command.levelledmobs.spawner.info.status-not-enabled"
            )
            return
        }

        if ("on".equals(args[2], ignoreCase = true)) {
            if (main.companion.spawnerCopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(playerId)
            }

            main.companion.spawnerInfoIds.add(playerId)
            showMessage("command.levelledmobs.spawner.info.enabled")
        } else if ("off".equals(args[2], ignoreCase = true)) {
            infoGotDisabled(playerId)
        }
    }

    private fun parseCopyCommand(
        args: Array<String>
    ) {
        val main = LevelledMobs.instance
        if (!commandSender!!.hasPermission("levelledmobs.command.spawner.copy")) {
            main.configUtils.sendNoPermissionMsg(commandSender!!)
            return
        }

        val playerId = (commandSender as Player).uniqueId

        if (args.size == 2) {
            showMessage(
                if (main.companion.spawnerCopyIds.contains(playerId)) "command.levelledmobs.spawner.copy.status-enabled"
                else "command.levelledmobs.spawner.copy.status-not-enabled"
            )
            return
        }

        if ("on".equals(args[2], ignoreCase = true)) {
            if (main.companion.spawnerInfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(playerId)
            }

            main.companion.spawnerCopyIds.add(playerId)
            showMessage("command.levelledmobs.spawner.copy.enabled")
        } else if ("off".equals(args[2], ignoreCase = true)) {
            copyGotDisabled(playerId)
        }
    }

    private fun copyGotDisabled(playerId: UUID) {
        LevelledMobs.instance.companion.spawnerCopyIds.remove(playerId)
        showMessage("command.levelledmobs.spawner.copy.disabled")
    }

    private fun infoGotDisabled(playerId: UUID) {
        LevelledMobs.instance.companion.spawnerInfoIds.remove(playerId)
        showMessage("command.levelledmobs.spawner.info.disabled")
    }

    private fun parseCreateCommand(args: Array<String>) {
        hadInvalidArg = false

        val info = CustomSpawnerInfo(messageLabel!!)
        if (commandSender is Player) {
            info.player = commandSender as Player
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
                        commandSender!!.sendMessage("Invalid spawn type: $foundValue")
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
                        showMessage("command.levelledmobs.spawner.no-player-specified")
                        return
                    }
                    try {
                        info.player = Bukkit.getPlayer(foundValue)
                    } catch (e: Exception) {
                        showMessage("common.player-offline", "%player%", foundValue)
                        return
                    }
                    if (info.player == null) {
                        showMessage("common.player-offline", "%player%", foundValue)
                        return
                    }
                }
            }
        }

        if (info.minLevel == -1 && info.maxLevel == -1) {
            showMessage("command.levelledmobs.spawner.no-level-specified")
            return
        }

        if (info.player == null) {
            showMessage("command.levelledmobs.spawner.no-player-specified")
            return
        }

        generateSpawner(info)
    }

    fun generateSpawner(
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
            showMessage("command.levelledmobs.spawner.inventory-full", info.player!!)
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
            Utils.logger.info(message[0].replace(LevelledMobs.instance.configUtils.prefix + " ", ""))
        }

        showMessage("command.levelledmobs.spawner.spawner-give-message", info.player!!)
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String> {
        if (!sender.hasPermission("levelledmobs.command.spawner")) {
            return mutableListOf()
        }

        if (args.size <= 2) {
            return mutableListOf("copy", "create", "info")
        }

        if ("create".equals(args[1], ignoreCase = true)) {
            return tabCompletionsCreate(args)
        } else if (("info".equals(args[1], ignoreCase = true) || "copy".equals(args[1], ignoreCase = true))
            && args.size == 3
        ) {
            return mutableListOf("on", "off")
        }

        return mutableListOf()
    }

    private fun tabCompletionsCreate(
        args: Array<String>
    ): MutableList<String> {
        if (args[args.size - 2].isNotEmpty()) {
            when (args[args.size - 2].lowercase(Locale.getDefault())) {
                "/spawntype" -> {
                    val entityNames: MutableList<String> = LinkedList()
                    for (entityType in EntityType.entries) {
                        entityNames.add(entityType.toString().lowercase(Locale.getDefault()))
                    }
                    return entityNames
                }

                "/delay" -> {
                    return mutableListOf("0")
                }

                "/minspawndelay" -> {
                    return mutableListOf("200")
                }

                "/maxspawndelay" -> {
                    return mutableListOf("800")
                }

                "/maxnearbyentities", "/requiredplayerrange" -> {
                    return mutableListOf("16")
                }

                "/spawncount", "/spawnrange" -> {
                    return mutableListOf("4")
                }

                "/giveplayer" -> {
                    val players: MutableList<String> = LinkedList()
                    for (player in Bukkit.getOnlinePlayers()) {
                        players.add(player.name)
                    }
                    players.sortWith(java.lang.String.CASE_INSENSITIVE_ORDER)
                    return players
                }
            }
        }

        return checkTabCompletion(allSpawnerOptions, args)
    }

    private enum class OperationEnum {
        CREATE,
        COPY,
        INFO
    }
}