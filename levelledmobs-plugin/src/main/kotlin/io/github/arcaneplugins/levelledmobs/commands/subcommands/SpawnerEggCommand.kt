package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.CommandExecutor
import java.util.Locale
import io.github.arcaneplugins.levelledmobs.LevelledMobs
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
 * Gives the user a specialized spawn egg that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.3.0
 */
object SpawnerEggCommand : SpawnerBaseClass() {
    fun createInstance(): CommandAPICommand{
        val commandName = "spawner-egg"
        val commandPermission = "levelledmobs.command.spawner-egg"
        val commandDescription = "Various commands for creating spawner egg."

        if (!LevelledMobs.instance.ver.isRunningPaper) {
            return CommandAPICommand(commandName)
                .withPermission(commandPermission)
                .withShortDescription(commandDescription)
                .withFullDescription(commandPermission)
                .executes(CommandExecutor { sender, _ ->
                    MessagesHelper.showMessage(sender, "command.levelledmobs.spawn_egg.no-paper")
                })
        }

        return CommandAPICommand(commandName)
            .withPermission(commandPermission)
            .withShortDescription(commandDescription)
            .withFullDescription(commandPermission)
            .executes(CommandExecutor { sender, args ->
                if (args.argsMap.isEmpty()) {
                    MessagesHelper.showMessage(sender, "command.levelledmobs.spawn_egg.usage")
                }
                else {
                    processResults(sender, args.rawArgs[0])
                }
            })
            .withOptionalArguments(
                ListArgumentBuilder<String>("values")
                    .allowAnyValue(true)
                    .allowDuplicates(true)
                    .withList { info -> buildTabSuggestions(allEggOptions, info) }
                    .withStringMapper()
                    .buildGreedy()
            )
    }

    private fun processResults(
        sender: CommandSender,
        input: String
    ){
        val args = Utils.splitStringWithQuotes(input)

        var hasGivePlayer = false
        for (i in 2 until args.size) {
            if ("/giveplayer".equals(args[i], ignoreCase = true)) {
                hasGivePlayer = true
                break
            }
        }

        if (!hasGivePlayer && sender !is Player) {
            MessagesHelper.showMessage(sender,"command.levelledmobs.spawn_egg.no-player")
            return
        }

        hadInvalidArg = false

        val info = CustomSpawnerInfo()
        if (sender is Player) {
            info.player = sender
        }

        // arguments with no values go here:
        for (i in 0 until args.size) {
            val arg = args[i]
            if ("/nolore".equals(arg, ignoreCase = true)) {
                info.noLore = true
                break
            }
        }

        for (i in 0 until allEggOptions.size - 1) {
            val mustBeANumber = (i > 4)
            val command = allEggOptions[i]
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
                "/entity" -> {
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.uppercase(Locale.getDefault()))
                    } catch (ignored: Exception) {
                        commandSender!!.sendMessage("Invalid spawn type: $foundValue")
                        return
                    }
                }

                "/minlevel" -> info.minLevel = foundValue.toInt()
                "/maxlevel" -> info.maxLevel = foundValue.toInt()
                "/giveplayer" -> {
                    if (foundValue.isEmpty()) {
                        MessagesHelper.showMessage(sender,"command.levelledmobs.spawn_egg.no-player-specified")
                        return
                    }
                    try {
                        info.player = Bukkit.getPlayer(foundValue)
                    } catch (e: Exception) {
                        MessagesHelper.showMessage(sender,"common.player-offline", "%player%", foundValue)
                        return
                    }
                    if (info.player == null) {
                        MessagesHelper.showMessage(sender, "common.player-offline", "%player%", foundValue)
                        return
                    }
                }
            }
        }

        if (info.minLevel == -1 || info.maxLevel == -1 || info.spawnType === EntityType.UNKNOWN) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawn_egg.no-level-specified")
            return
        }

        if (info.player == null) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawn_egg.no-player")
            return
        }

        generateEgg(sender, info)
    }

    private val allEggOptions = mutableListOf(
        "/name", "/customdropid", "/entity", "/giveplayer", "/lore",
        "/minlevel", "/maxlevel","/nolore"
    )

    private fun generateEgg(
        sender: CommandSender,
        info: CustomSpawnerInfo
    ) {
        if (info.customName != null) {
            info.customName = colorizeAll(info.customName)
        }

        val materialName: String = info.spawnType.name + "_SPAWN_EGG"
        val material = Material.getMaterial(materialName)
        if (material == null) {
            // should never see this message:
            commandSender!!.sendMessage("Invalid material: $materialName")
            return
        }
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) {
            setMetaItems(meta, info, "LM Spawn Egg")

            meta.persistentDataContainer
                .set(NamespacedKeys.spawnerEgg, PersistentDataType.INTEGER, 1)
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
            if (info.spawnType !== EntityType.UNKNOWN) {
                meta.persistentDataContainer
                    .set(
                        NamespacedKeys.keySpawnerSpawnType, PersistentDataType.STRING,
                        info.spawnType.toString()
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

        var useInvSlotNum: Int = info.player!!.inventory.heldItemSlot
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
            "command.levelledmobs.spawn_egg.give-message-console",
            arrayOf("%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"),
            arrayOf(
                info.minLevel.toString(), info.maxLevel.toString(), playerName,
                info.spawnType.name
            )
        )

        if (message.isNotEmpty()) {
            val consoleMsg = message[0]
                .replace(LevelledMobs.instance.configUtils.prefix + " ", "&r")
            Log.inf(consoleMsg)
        }

        showMessage(
            "command.levelledmobs.spawn_egg.give-message",
            arrayOf("%minlevel%", "%maxlevel%", "%playername%", "%entitytype%"),
            arrayOf(
                info.minLevel.toString(), info.maxLevel.toString(), playerName,
                info.spawnType.name
            ),
            info.player!!
        )
    }
}