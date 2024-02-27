package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.SuggestionInfo
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.ZombieVillager
import org.bukkit.metadata.FixedMetadataValue

/**
 * Allows you to kill LevelledMobs with various options including all levelled mobs, specific worlds
 * or levelled mobs in your proximity
 *
 * @author stumper66
 * @since 2.0
 */
object KillSubcommand {
    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("kill")
            .withPermission("levelledmobs.command.kill")
            .withShortDescription("Various commands for killing LevelledMobs mobs.")
            .withFullDescription("Various commands for killing LevelledMobs mobs.")
            .executes(CommandExecutor { sender, _ -> MessagesHelper.showMessage(sender, "levelledmobs.command.kill.usage") })
            .withSubcommands(
                CommandAPICommand("all")
                    .withPermission("levelledmobs.command.kill.all")
                    .executes(CommandExecutor { sender, args -> processCmd(sender, args, true) })
                    .withOptionalArguments(
                        ListArgumentBuilder<String>("values")
                            .skipListValidation(true)
                            .withList { info -> buildTabSuggestions(info) }
                            .withStringMapper()
                            .buildGreedy()
            ))
            .withSubcommands(
                CommandAPICommand("near")
                    .withPermission("levelledmobs.command.kill.all")
                    .executes(CommandExecutor { sender, args -> processCmd(sender, args, false) })
                    .withOptionalArguments(
                        ListArgumentBuilder<String>("values")
                            .skipListValidation(true)
                            .withList { info -> buildTabSuggestions(info) }
                            .withStringMapper()
                            .buildGreedy()

            ))
    }

    private fun processCmd(
        sender: CommandSender,
        input: CommandArguments,
        isAll: Boolean
    ){
        val values = input.rawArgsMap["values"]
        if (values == null) {
            if (sender is Player) {
                if (isAll)
                    parseKillAll(sender, mutableListOf(sender.world), false, null)
                else
                    MessagesHelper.showMessage(sender, "command.levelledmobs.kill.near.usage")
            } else {
                MessagesHelper.showMessage(sender,
                    if (isAll) "command.levelledmobs.kill.all.usage-console"
                    else "command.levelledmobs.kill.near.usage-console")
            }
            return
        }

        val args = Utils.splitStringWithQuotes(values)
        var useNoDrops = false
        val rl = getLevelFromCommand(sender, args)
        val newArgs = mutableListOf<String>()
        if (rl != null) {
            if (rl.hadInvalidArguments) {
                return
            }
        }

        var rlFlag = -1
        for (i in 0..<args.size) {
            val checkArg = args[i]
            if ("/nodrops".equals(checkArg, ignoreCase = true)) {
                useNoDrops = true
            }
            else if ("/levels".equals(checkArg, ignoreCase = true)) {
                rlFlag = i + 1
            }
            else if (i != rlFlag)
                newArgs.add(checkArg)
        }

        val options = Options(newArgs, useNoDrops, rl)

        if (isAll)
            processKillAll(sender, options)
        else
            processKillNear(sender, options)
    }

    private fun processKillAll(
        sender: CommandSender,
        opts: Options
    ){
        var useArg = ""
        for (i in 0..<opts.args.size){
            if (opts.args[i].startsWith("/")) continue
            useArg = opts.args[i]
        }

        if (useArg.isEmpty()) {
            if (sender is Player) {
                parseKillAll(sender, mutableListOf(sender.world), opts.useNoDrops, opts.requestedLevel)
            } else {
                MessagesHelper.showMessage(sender, "command.levelledmobs.kill.all.usage-console")
            }
        } else {
            if (useArg == "*") {
                parseKillAll(sender, Bukkit.getWorlds(), opts.useNoDrops, opts.requestedLevel)
                return
            }

            val world = Bukkit.getWorld(useArg)
            if (world == null) {
                MessagesHelper.showMessage(sender,
                    "command.levelledmobs.kill.all.invalid-world", "%world%",
                    useArg
                )
                return
            }
            parseKillAll(sender, mutableListOf(world), opts.useNoDrops, opts.requestedLevel)
        }
    }

    private fun processKillNear(
        sender: CommandSender,
        opts: Options
    ){
        if (sender !is BlockCommandSender && sender !is Player) {
            MessagesHelper.showMessage(sender, "common.players-only")
            return
        }

        if (opts.args.isEmpty()){
            MessagesHelper.showMessage(sender, "command.levelledmobs.kill.near.usage")
            return
        }

        var radius: Int
        try {
            radius = opts.args[0].toInt()
        } catch (exception: NumberFormatException) {
            MessagesHelper.showMessage(sender,
                "command.levelledmobs.kill.near.invalid-radius", "%radius%",
                opts.args[0]
            )
            return
        }

        val maxRadius = 1000
        if (radius > maxRadius) {
            radius = maxRadius
            MessagesHelper.showMessage(sender,
                "command.levelledmobs.kill.near.invalid-radius-max",
                "%maxRadius%", maxRadius.toString()
            )
        }

        val minRadius = 1
        if (radius < minRadius) {
            radius = minRadius
            MessagesHelper.showMessage(sender,
                "command.levelledmobs.kill.near.invalid-radius-min",
                "%minRadius%", minRadius.toString()
            )
        }

        var killed = 0
        var skipped = 0
        val mobsToKill: Collection<Entity>
        if (sender is BlockCommandSender) {
            val block = sender.block
            mobsToKill = block.world
                .getNearbyEntities(block.location, radius.toDouble(), radius.toDouble(), radius.toDouble())
        } else {
            mobsToKill = (sender as Player).getNearbyEntities(
                radius.toDouble(),
                radius.toDouble(),
                radius.toDouble()
            )
        }

        for (entity in mobsToKill) {
            if (entity !is LivingEntity) continue

            if (!LevelledMobs.instance.levelInterface.isLevelled(entity)) continue

            if (skipKillingEntity(entity, opts.requestedLevel)) {
                skipped++
                continue
            }

            entity.setMetadata(
                "noCommands",
                FixedMetadataValue(LevelledMobs.instance, 1)
            )

            if (opts.useNoDrops) {
                entity.remove()
            } else {
                entity.health = 0.0
            }
            killed++
        }

        MessagesHelper.showMessage(sender,
            "command.levelledmobs.kill.near.success",
            arrayOf("%killed%", "%skipped%", "%radius%"),
            arrayOf(
                killed.toString(), skipped.toString(),
                radius.toString()
            )
        )
    }

    private fun buildTabSuggestions(
        info: SuggestionInfo<CommandSender>
    ): MutableList<String> {
        val args = Utils.splitStringWithQuotes(info.currentInput)
        val prevArgs = Utils.splitStringWithQuotes(info.previousArgs.fullInput())
        val lastArg = prevArgs.last()

        var containsNoDrops = false
        var containsLevels = false
        var containsWorld = false

        if ("/levels".equals(lastArg, ignoreCase = true))
            return Utils.oneToNine

        for (i in 3..<args.size) {
            val arg = args[i]

            if ("/nodrops".equals(arg, ignoreCase = true)) {
                containsNoDrops = true
            } else if ("/levels".equals(arg, ignoreCase = true)) {
                containsLevels = true
            }
            else if (!arg.startsWith("/") && !Utils.isInteger(arg))
                containsWorld = true
        }

        if ("all".equals(args[2], ignoreCase = true) && args.size <= 7) {
            val worlds = mutableListOf<String>()

            if (!containsNoDrops) {
                worlds.add("/nodrops")
            }
            if (!containsLevels) {
                worlds.add("/levels")
            }
            if (!containsWorld) {
                for (world in Bukkit.getWorlds()) {
                    worlds.add("*")
                    if (LevelledMobs.instance.rulesManager.getRuleIsWorldAllowedInAnyRule(world)) {
                        worlds.add(world.name)
                    }
                }
            }

            return worlds
        }
        else if ("near".equals(args[2], ignoreCase = true)) {
            if (args.size == 3) return Utils.oneToNine

            if ("/levels".equals(lastArg, ignoreCase = true)) {
                return mutableListOf("/levels")
            }
        }

        val result = mutableListOf<String>()
        if (!containsNoDrops) {
            result.add("/nodrops")
        }
        if (!containsLevels) {
            result.add("/levels")
        }

        return result
    }

    private fun getLevelFromCommand(
        sender: CommandSender,
        args: MutableList<String>
    ): RequestedLevel? {
        var rangeSpecifiedFlag = -1

        for (i in args.indices) {
            if ("/levels".equals(args[i], ignoreCase = true)) {
                rangeSpecifiedFlag = i + 1
            }
        }

        if (rangeSpecifiedFlag <= 0) {
            return null
        }

        val rl = RequestedLevel()
        if (args.size <= rangeSpecifiedFlag) {
            sender.sendMessage("No value was specified for /levels")
            rl.hadInvalidArguments = true
            return rl
        }

        val value = args[rangeSpecifiedFlag]
        if (!rl.setLevelFromString(value)) {
            sender.sendMessage("Invalid number or range specified for /levels")
            rl.hadInvalidArguments = true
        }

        return rl
    }

    private fun parseKillAll(
        sender: CommandSender,
        worlds: MutableList<World>,
        useNoDrops: Boolean,
        rl: RequestedLevel?
    ) {
        var killed = 0
        var skipped = 0

        for (world in worlds) {
            for (entity in world.entities) {
                if (entity !is LivingEntity) {
                    continue
                }
                if (!LevelledMobs.instance.levelInterface.isLevelled(entity)) {
                    continue
                }

                if (skipKillingEntity(entity, rl)) {
                    skipped++
                    continue
                }

                entity.setMetadata("noCommands", FixedMetadataValue(LevelledMobs.instance, 1))

                if (useNoDrops) {
                    entity.remove()
                } else {
                    entity.health = 0.0
                }

                killed++
            }
        }

        MessagesHelper.showMessage(sender,
            "command.levelledmobs.kill.all.success",
            arrayOf("%killed%", "%skipped%", "%worlds%"),
            arrayOf(
                killed.toString(), skipped.toString(),
                worlds.size.toString()
            )
        )
    }

    private fun skipKillingEntity(
        livingEntity: LivingEntity,
        rl: RequestedLevel?
    ): Boolean {
        val main = LevelledMobs.instance
        @Suppress("DEPRECATION")
        if (livingEntity.customName != null && main.helperSettings.getBoolean(
                "kill-skip-conditions.nametagged"
            )
        ) {
            return true
        }

        if (rl != null) {
            val mobLevel: Int = main.levelInterface.getLevelOfMob(livingEntity)
            if (mobLevel < rl.levelMin || mobLevel > rl.levelMax) {
                return true
            }
        }

        // Tamed
        if (livingEntity is Tameable && livingEntity.isTamed
            && main.helperSettings.getBoolean( "kill-skip-conditions.tamed")
        ) {
            return true
        }

        // Leashed
        if (livingEntity.isLeashed && main.helperSettings.getBoolean(
                "kill-skip-conditions.leashed"
            )
        ) {
            return true
        }

        // Converting zombie villager
        return livingEntity.type == EntityType.ZOMBIE_VILLAGER &&
                (livingEntity as ZombieVillager).isConverting &&
                main.helperSettings.getBoolean(
                    "kill-skip-conditions.convertingZombieVillager"
                )
    }

    private class Options(
        val args: MutableList<String>,
        val useNoDrops: Boolean,
        val requestedLevel: RequestedLevel?
    )

}