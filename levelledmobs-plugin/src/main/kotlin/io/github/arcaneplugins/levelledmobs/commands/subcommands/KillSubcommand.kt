package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.util.MiscUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.TreeSet
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.AbstractVillager
import org.bukkit.entity.Entity
import org.bukkit.entity.Hoglin
import org.bukkit.entity.Husk
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.PigZombie
import org.bukkit.entity.PiglinAbstract
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Tameable
import org.bukkit.entity.Zombie
import org.bukkit.entity.ZombieVillager
import org.bukkit.metadata.FixedMetadataValue

/**
 * Allows you to kill LevelledMobs with various options including all levelled mobs, specific worlds
 * or levelled mobs in your proximity
 *
 * @author stumper66
 * @since 2.0
 */
object KillSubcommand : CommandBase("levelledmobs.command.kill") {
    override val description = "Various commands for killing LevelledMobs mobs."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack> {
        return createLiteralCommand("kill")
            .executes { ctx -> processCmd(ctx, true)
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("all")
                .then(createGreedyStringArgument("all")
                    .suggests { _, builder -> buildTabSuggestions(builder) }
                    .executes { ctx -> processCmd(ctx, true)
                        return@executes Command.SINGLE_SUCCESS })
            .executes { ctx -> processCmd(ctx, true)
                return@executes Command.SINGLE_SUCCESS
            })
            .then(createLiteralCommand("near")
                .then(createGreedyStringArgument("near")
                    .suggests { _, builder -> buildTabSuggestions(builder) }
                    .executes { ctx -> processCmd(ctx, false)
                        return@executes Command.SINGLE_SUCCESS })
            .executes { ctx -> processCmd(ctx, true)
                return@executes Command.SINGLE_SUCCESS
            })
            .build()
    }

    private fun processCmd(
        ctx: CommandContext<CommandSourceStack>,
        isAll: Boolean
    ){
        val sender = ctx.source.sender
        commandSender = sender
        //TODO: make this work in Folia
        if (LevelledMobs.instance.ver.isRunningFolia) {
            sender.sendMessage("Sorry this command doesn't work in Folia")
            return
        }

        val values = ctx.input
        if (values == null) {
            if (sender is Player) {
                if (isAll)
                    processKillAll(mutableListOf(sender.world), false, null)
                else
                    showMessage("command.levelledmobs.kill.near.usage")
            } else {
                showMessage(
                    if (isAll) "command.levelledmobs.kill.all.usage-console"
                    else "command.levelledmobs.kill.near.usage-console")
            }
            return
        }

        val args = splitStringWithQuotes(values, false)
        var useNoDrops = false
        val rl = getLevelFromCommand(sender, args)
        val newArgs = mutableListOf<String>()
        if (rl?.hadInvalidArguments ?: false) return

        var rlFlag = -1
        for (i in 3..<args.size) {
            val checkArg = args[i]
            if ("/nodrops".equals(checkArg, ignoreCase = true))
                useNoDrops = true
            else if ("/levels".equals(checkArg, ignoreCase = true))
                rlFlag = i + 1
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
            if (sender is Player)
                processKillAll(mutableListOf(sender.world), opts.useNoDrops, opts.requestedLevel)
            else
                showMessage("command.levelledmobs.kill.all.usage-console")
        } else {
            if (useArg == "*") {
                processKillAll(Bukkit.getWorlds(), opts.useNoDrops, opts.requestedLevel)
                return
            }

            val world = Bukkit.getWorld(useArg)
            if (world == null) {
                showMessage(
                    "command.levelledmobs.kill.all.invalid-world", "%world%",
                    useArg
                )
                return
            }
            processKillAll(mutableListOf(world), opts.useNoDrops, opts.requestedLevel)
        }
    }

    private fun processKillNear(
        sender: CommandSender,
        opts: Options
    ){
        if (sender !is BlockCommandSender && sender !is Player) {
            showMessage("common.players-only")
            return
        }

        if (opts.args.isEmpty()){
            showMessage("command.levelledmobs.kill.near.usage")
            return
        }

        var radius: Int
        try {
            radius = opts.args[0].toInt()
        } catch (_: NumberFormatException) {
            showMessage("command.levelledmobs.kill.near.invalid-radius", "%radius%",
                opts.args.first()
            )
            return
        }

        val maxRadius = 1000
        if (radius > maxRadius) {
            radius = maxRadius
            showMessage("command.levelledmobs.kill.near.invalid-radius-max",
                "%maxRadius%", maxRadius.toString()
            )
        }

        val minRadius = 1
        if (radius < minRadius) {
            radius = minRadius
            showMessage("command.levelledmobs.kill.near.invalid-radius-min",
                "%minRadius%", minRadius.toString()
            )
        }

        var killed = 0
        var skipped = 0
        val mobsToKill: Collection<Entity>

        if (sender is BlockCommandSender) {
            val block = sender.block
            val finalRadius = MiscUtils.retrieveLoadedChunkRadius(block.location, radius.toDouble())
            mobsToKill = block.world
                .getNearbyEntities(block.location, finalRadius, finalRadius, finalRadius)
        } else {
            val finalRadius = MiscUtils.retrieveLoadedChunkRadius((sender as Player).location, radius.toDouble())
            mobsToKill = sender.getNearbyEntities(finalRadius, finalRadius, finalRadius)
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

            if (opts.useNoDrops)
                entity.remove()
            else
                entity.health = 0.0

            killed++
        }

        showMessage("command.levelledmobs.kill.near.success",
            mutableListOf("%killed%", "%skipped%", "%radius%"),
            mutableListOf(
                killed.toString(), skipped.toString(),
                radius.toString()
            )
        )
    }

    private fun buildTabSuggestions(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {

        val args = splitStringWithQuotes(builder.input, true)
        if (args.size == 2) return builder.buildFuture()
        val lastArg = args[args.size - 1]
        val prefix = StringBuilder()
        val existingItems = mutableListOf<String>()
        var containsNoDrops = false
        var containsLevels = false
        var containsWorld = false
        val hasRemaining = builder.remaining.isNotEmpty() && !builder.remaining.endsWith(' ')

        if ("/levels".equals(lastArg, ignoreCase = true)) {
            Utils.getOneToNineSuggestions(builder)
            return builder.buildFuture()
        }

        for (i in 3..<args.size) {
            val arg = args[i]

            existingItems.add(arg.lowercase())
            prefix.append(arg).append(' ')

            if ("/nodrops".equals(arg, ignoreCase = true))
                containsNoDrops = true
            else if ("/levels".equals(arg, ignoreCase = true))
                containsLevels = true
            else if (!arg.startsWith("/") && !hasRemaining && !Utils.isInteger(arg))
                containsWorld = true
        }

        if ("all".equals(args[2], ignoreCase = true) && args.size <= 7) {
            val commandsList: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
            commandsList.addAll(mutableListOf("/nodrops", "/levels"))
            if (args.isEmpty()) return builder.buildFuture()

            args.forEach { arg ->
                if (commandsList.contains(arg))
                    commandsList.remove(arg)
            }

            commandsList.forEach { command -> builder.suggest("$prefix$command") }

            if (!containsWorld) {
                for (world in Bukkit.getWorlds()) {
                    builder.suggest("*")
                    if (LevelledMobs.instance.rulesManager.getRuleIsWorldAllowedInAnyRule(world) &&
                        !existingItems.contains(world.name))
                            builder.suggest("$prefix${world.name}")
                }
            }

            return builder.buildFuture()
        }
        else if ("near".equals(args[2], ignoreCase = true)) {
            if (args.size == 3) {
                Utils.getOneToNineSuggestions(builder)
                return builder.buildFuture()
            }

            if ("/levels".equals(lastArg, ignoreCase = true)) {
                builder.suggest("/levels")
                return builder.buildFuture()
            }
        }

        if (!containsNoDrops)
            builder.suggest("/nodrops")

        if (!containsLevels)
            builder.suggest("/levels")

        return builder.buildFuture()
    }

    private fun getLevelFromCommand(
        sender: CommandSender,
        args: MutableList<String>
    ): RequestedLevel? {
        var rangeSpecifiedFlag = -1

        for (i in args.indices) {
            if ("/levels".equals(args[i], ignoreCase = true))
                rangeSpecifiedFlag = i + 1
        }

        if (rangeSpecifiedFlag <= 0)
            return null

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

    private fun processKillAll(
        worlds: MutableList<World>,
        useNoDrops: Boolean,
        rl: RequestedLevel?
    ) {
        LevelledMobs.instance.mobsQueueManager.clearQueue()
        var killed = 0
        var skipped = 0

        for (world in worlds) {
            for (entity in world.entities) {
                if (entity !is LivingEntity) continue

                if (!LevelledMobs.instance.levelInterface.isLevelled(entity))
                    continue

                if (skipKillingEntity(entity, rl)) {
                    skipped++
                    continue
                }

                entity.setMetadata("noCommands", FixedMetadataValue(LevelledMobs.instance, 1))

                if (useNoDrops)
                    entity.remove()
                else
                    entity.health = 0.0

                killed++
            }
        }

        showMessage("command.levelledmobs.kill.all.success",
            mutableListOf("%killed%", "%skipped%", "%worlds%"),
            mutableListOf(
                killed.toString(), skipped.toString(),
                worlds.size.toString()
            )
        )
    }

    private fun skipKillingEntity(
        livingEntity: LivingEntity,
        rl: RequestedLevel?
    ): Boolean {
        val skc = MainCompanion.instance.killSkipConditions
        @Suppress("DEPRECATION")
        if (livingEntity.customName != null && skc.isNametagged
        ) {
            return true
        }

        if (rl != null) {
            val mobLevel = LevelledMobs.instance.levelInterface.getLevelOfMob(livingEntity)
            if (mobLevel < rl.levelMin || mobLevel > rl.levelMax)
                return true
        }

        // Tamed
        if (livingEntity is Tameable && skc.isTamed)
            return true

        // Leashed
        if (livingEntity.isLeashed && skc.isLeashed)
            return true

        if (livingEntity is AbstractVillager && skc.isVillager)
            return true

        if (skc.entityTypes != null && !skc.entityTypes!!.isEmpty()){
            val et = skc.entityTypes!!
            if (et.includeAll) return false
            if (et.excludeAll) return true
            val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
            try{
                if (et.isIncludedInList(lmEntity.nameIfBaby, lmEntity) ||
                    et.includedList.contains("baby_") && lmEntity.isBabyMob)
                    return true
            }
            finally {
                lmEntity.free()
            }
        }

        return (skc.isTransforming && isMobConverting(livingEntity))
    }

    private fun isMobConverting(mob: LivingEntity): Boolean{
        if (mob is Hoglin && mob.isConverting) return true
        if (mob is Husk && mob.isConverting) return true
        if (mob is PiglinAbstract && mob.isConverting) return true
        if (mob is PigZombie && mob.isConverting) return true
        if (mob is Skeleton && mob.isConverting) return true
        if (mob is Zombie && mob.isConverting) return true
        return (mob is ZombieVillager && mob.isConverting)
    }

    private class Options(
        val args: MutableList<String>,
        val useNoDrops: Boolean,
        val requestedLevel: RequestedLevel?
    )
}