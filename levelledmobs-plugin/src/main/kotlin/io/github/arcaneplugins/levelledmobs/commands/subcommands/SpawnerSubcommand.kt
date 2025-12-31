package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Gives the user a specialized spawner that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.0.0
 */
object SpawnerSubcommand : SpawnerBaseClass("levelledmobs.command.spawner") {
    override val description = "Various commands for creating spawner cubes."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack> {
        return createLiteralCommand("spawner")
            .executes { ctx -> commandSender = ctx.source.sender
                showMessage("command.levelledmobs.spawner.usage")
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("create")
                .then(createGreedyStringArgument("values")
                    .suggests { ctx, builder -> buildTabSuggestions(allSpawnerOptions, ctx, builder) }
                    .executes { ctx -> processResults(ctx, OperationEnum.CREATE)
                        return@executes Command.SINGLE_SUCCESS
                    })
            .executes { ctx -> processResults(ctx, OperationEnum.CREATE)
                return@executes Command.SINGLE_SUCCESS
            })
            .then(createLiteralCommand("copy")
                .then(createStringArgument("value")
                    .suggests { _, builder -> builder.suggest("on").suggest("off").buildFuture() }
                    .executes { ctx -> processResults(ctx, OperationEnum.COPY)
                        return@executes Command.SINGLE_SUCCESS
                    })
            .executes { ctx -> processResults(ctx, OperationEnum.COPY)
                return@executes Command.SINGLE_SUCCESS
            })
            .then(createLiteralCommand("info")
                .then(createStringArgument("value")
                    .suggests { _, builder -> builder.suggest("on").suggest("off").buildFuture() }
                    .executes { ctx -> processResults(ctx, OperationEnum.INFO)
                        return@executes Command.SINGLE_SUCCESS
                    })
            .executes { ctx -> processResults(ctx, OperationEnum.INFO)
                return@executes Command.SINGLE_SUCCESS
            })
            .build()
    }

    private fun processResults(
        ctx: CommandContext<CommandSourceStack>,
        operation: OperationEnum
    ) {
        commandSender = ctx.source.sender
        val sender = ctx.source.sender
        val args = splitStringWithQuotes(ctx.input, false)

        if (args.size <= 2) {
            when (operation){
                OperationEnum.CREATE -> {
                    showMessage("command.levelledmobs.spawner.usage")
                }
                OperationEnum.COPY -> {
                    val playerId = (sender as Player).uniqueId
                    showMessage(
                        if (MainCompanion.instance.spawnerCopyIds.contains(playerId))
                            "command.levelledmobs.spawner.copy.status-enabled"
                        else
                            "command.levelledmobs.spawner.copy.status-not-enabled"
                    )
                }
                OperationEnum.INFO -> {
                    val playerId = (sender as Player).uniqueId
                    showMessage(
                        if (MainCompanion.instance.spawnerInfoIds.contains(playerId))
                            "command.levelledmobs.spawner.info.status-enabled"
                        else
                            "command.levelledmobs.spawner.info.status-not-enabled"
                    )
                }
            }
            return
        }

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
            OperationEnum.COPY -> parseCopyCommand(ctx)
            OperationEnum.INFO -> parseInfoCommand(ctx)
        }
    }

    private fun parseInfoCommand(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val sender = ctx.source.sender
        commandSender = sender
        val playerId = (sender as Player).uniqueId
        val main = LevelledMobs.instance
        val value = getStringArgument(ctx, "value")

        if ("on".equals(value, ignoreCase = true)) {
            if (main.mainCompanion.spawnerCopyIds.contains(playerId)) {
                // can't have both enabled.  We'll disable copy first
                copyGotDisabled(playerId)
            }

            main.mainCompanion.spawnerInfoIds.add(playerId)
            MessagesHelper.showMessage(sender, "command.levelledmobs.spawner.info.enabled")
        }
        else if ("off".equals(value, ignoreCase = true))
            infoGotDisabled(playerId)
    }

    private val allSpawnerOptions = mutableListOf(
        "/name", "/customdropid", "/spawntype", "/giveplayer",
        "/lore", "/minlevel", "/maxlevel", "/delay",
        "/maxnearbyentities", "/minspawndelay", "/maxspawndelay",
        "/requiredplayerrange", "/spawncount", "/spawnrange", "/nolore"
    )

    private fun parseCopyCommand(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val main = LevelledMobs.instance
        val sender = ctx.source.sender
        val playerId = (sender as Player).uniqueId
        val value = getStringArgument(ctx, "value")

        if ("on".equals(value, ignoreCase = true)) {
            if (main.mainCompanion.spawnerInfoIds.contains(playerId)) {
                // can't have both enabled.  We'll disable info first
                infoGotDisabled(playerId)
            }

            main.mainCompanion.spawnerCopyIds.add(playerId)
            showMessage("command.levelledmobs.spawner.copy.enabled")
        }
        else if ("off".equals(value, ignoreCase = true))
            copyGotDisabled(playerId)

    }

    private fun copyGotDisabled(playerId: UUID) {
        MainCompanion.instance.spawnerCopyIds.remove(playerId)
        showMessage("command.levelledmobs.spawner.copy.disabled")
    }

    private fun infoGotDisabled(playerId: UUID) {
        MainCompanion.instance.spawnerInfoIds.remove(playerId)
        showMessage("command.levelledmobs.spawner.info.disabled")
    }

    private fun parseCreateCommand(
        sender: CommandSender,
        args: MutableList<String>
    ) {
        hadInvalidArg = false

        val info = CustomSpawnerInfo(false)
        if (sender is Player) info.player = sender

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
            if (hadInvalidArg) return

            if (foundValue.isNullOrEmpty()) continue

            when (command) {
                "/name" -> info.customName = foundValue
                "/customdropid" -> info.customDropId = foundValue
                "/lore" -> info.customLore = foundValue
                "/spawntype" -> {
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.uppercase())
                    } catch (_: Exception) {
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
                    } catch (_: Exception) {
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
        if (info.customName != null)
            info.customName = colorizeAll(info.customName)

        val item = ItemStack(Material.SPAWNER)
        val meta = item.itemMeta
        val defaultName = "LM Spawner"
        setMetadata(item, info, defaultName)
        setMetaItems(meta, info, defaultName)
        giveItemToPlayer(item, info)
    }

    private enum class OperationEnum {
        CREATE,
        COPY,
        INFO
    }
}