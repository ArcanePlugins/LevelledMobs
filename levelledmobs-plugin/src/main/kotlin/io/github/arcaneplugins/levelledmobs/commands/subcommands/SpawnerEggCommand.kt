package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Gives the user a specialized spawn egg that only spawns mobs within certain level criteria
 *
 * @author stumper66
 * @since 3.3.0
 */
object SpawnerEggCommand : SpawnerBaseClass("levelledmobs.command.spawner-egg") {
    override val description = "Various commands for creating spawner egg."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack>{
        val commandName = "spawner-egg"

        if (!LevelledMobs.instance.ver.isRunningPaper) {
            return createLiteralCommand(commandName)
                .executes { ctx -> commandSender = ctx.source.sender
                    showMessage("command.levelledmobs.spawn_egg.no-paper")
                    return@executes Command.SINGLE_SUCCESS
                }
                .build()
        }

        return createLiteralCommand(commandName)
            .executes { ctx -> commandSender = ctx.source.sender
                showMessage("command.levelledmobs.spawn_egg.usage")
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createGreedyStringArgument("values")
                .suggests { ctx, builder -> buildTabSuggestions(allEggOptions, ctx, builder) }
                .executes { ctx -> processResults(ctx)
                    return@executes Command.SINGLE_SUCCESS })
            .build()
    }

    private fun processResults(
        ctx: CommandContext<CommandSourceStack>
    ){
        this.commandSender = ctx.source.sender
        val args = splitStringWithQuotes(ctx.input, false)

        var hasGivePlayer = false
        for (i in 2 until args.size) {
            if ("/giveplayer".equals(args[i], ignoreCase = true)) {
                hasGivePlayer = true
                break
            }
        }

        if (!hasGivePlayer && commandSender !is Player) {
            showMessage("command.levelledmobs.spawn_egg.no-player")
            return
        }

        hadInvalidArg = false

        val info = CustomSpawnerInfo(true)
        if (commandSender is Player) info.player = commandSender!! as Player

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

            if (hadInvalidArg) return
            if (foundValue.isNullOrEmpty()) continue

            when (command) {
                "/name" -> info.customName = foundValue
                "/customdropid" -> info.customDropId = foundValue
                "/lore" -> info.customLore = foundValue
                "/entity" -> {
                    try {
                        info.spawnType = EntityType.valueOf(foundValue.uppercase())
                    } catch (_: Exception) {
                        commandSender!!.sendMessage("Invalid spawn type: $foundValue")
                        return
                    }
                }

                "/minlevel" -> info.minLevel = foundValue.toInt()
                "/maxlevel" -> info.maxLevel = foundValue.toInt()
                "/giveplayer" -> {
                    if (foundValue.isEmpty()) {
                        showMessage("command.levelledmobs.spawn_egg.no-player-specified")
                        return
                    }
                    try {
                        info.player = Bukkit.getPlayer(foundValue)
                    } catch (_: Exception) {
                        showMessage("common.player-offline", "%player%", foundValue)
                        return
                    }
                    if (info.player == null) {
                        showMessage( "common.player-offline", "%player%", foundValue)
                        return
                    }
                }
            }
        }

        if (info.minLevel == -1 || info.maxLevel == -1 || info.spawnType === EntityType.UNKNOWN) {
            showMessage("command.levelledmobs.spawn_egg.no-level-specified")
            return
        }

        if (info.player == null) {
            showMessage("command.levelledmobs.spawn_egg.no-player")
            return
        }

        generateEgg(info)
    }

    private val allEggOptions = mutableListOf(
        "/name", "/customdropid", "/entity", "/giveplayer", "/lore",
        "/minlevel", "/maxlevel","/nolore"
    )

    private fun generateEgg(
        info: CustomSpawnerInfo
    ) {
        if (info.customName != null)
            info.customName = colorizeAll(info.customName)

        val materialName = info.spawnType.name + "_SPAWN_EGG"
        val material = Material.getMaterial(materialName)
        if (material == null) {
            // should never see this message:
            commandSender!!.sendMessage("Invalid material: $materialName")
            return
        }
        val item = ItemStack(material)
        setMetadata(item, info, "LM Spawn Egg")
        giveItemToPlayer(item, info)
    }
}