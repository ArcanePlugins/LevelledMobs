package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.SuggestionInfo
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import java.util.Locale
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.misc.LivingEntityPlaceholder
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.CommandBlock
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max
import kotlin.math.min

/**
 * Summons a levelled mob with a specific level and criteria
 *
 * @author stumper66
 * @author lokka30
 * @since v2.0.0
 */
object SummonSubcommand {
    fun createInstance(): CommandAPICommand {
        return CommandAPICommand("summon")
            .withPermission("levelledmobs.command.summon")
            .withShortDescription("Various commands for summoning mobs.")
            .withFullDescription("Various commands for summoning mobs.")
            .executes(CommandExecutor { sender, args -> processCmd(sender, args) })
            .withOptionalArguments(IntegerArgument("number")
                .replaceSuggestions(ArgumentSuggestions.strings(Utils.oneToNine)))
            .withOptionalArguments(StringArgument("mobtype")
                .replaceSuggestions(ArgumentSuggestions.strings(getEntityNames())))
            .withOptionalArguments(IntegerArgument("moblevel")
            .replaceSuggestions(ArgumentSuggestions.strings(Utils.oneToNine)))
            .withOptionalArguments(StringArgument("designator")
                .replaceSuggestions(ArgumentSuggestions.strings("here", "at-player", "at-location")))
            .withOptionalArguments(
                ListArgumentBuilder<String>("values")
                    .skipListValidation(true)
                    .withList { info -> buildTabSuggestions(info) }
                    .withStringMapper()
                    .buildGreedy()
            )
    }

    private fun processCmd(
        sender: CommandSender,
        args: CommandArguments
    ){
        val amount = args.get("number") as? Int
        val mobLevel = args.get("moblevel") as? Int
        val mobType = args.get("mobtype") as? String
        val designator = args.get("designator") as? String?: "here"
        val values = args.rawArgsMap["values"]

        if (amount == null || mobLevel == null || mobType == null) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.summon.usage")
            return
        }

        val entityType: EntityType
        try {
            entityType = EntityType.valueOf(mobType.uppercase(Locale.getDefault()))
        } catch (ex: IllegalArgumentException) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.summon.invalid-entity-type", "%entityType%", mobType)
            return
        }

        val requestedLevel = RequestedLevel()
        requestedLevel.level = mobLevel
        var location = when (sender) {
            is Player -> sender.location
            is CommandBlock -> sender.location
            else -> null
        }
        var nbtData: String? = null

        val miscArgs = if (values != null) Utils.splitStringWithQuotes(values) else mutableListOf()
        // 0           1   2   3   4
        // <nbtdata>
        // at-player   <player>
        // at-location <x> <y> <z> <world>
        for (i in 0..<miscArgs.size){
            val arg = miscArgs[i]
            if (arg.startsWith("{") && arg.endsWith("}"))
                nbtData = arg
        }

        when (designator.lowercase()){
            "here" -> {
                if (location == null){
                    MessagesHelper.showMessage(sender, "command.levelledmobs.summon.here.usage")
                    return
                }

                val lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                    entityType, location
                )
                val options = SummonMobOptions(lmPlaceHolder, sender)
                options.amount = amount
                options.requestedLevel = requestedLevel
                options.summonType = SummonType.HERE
                options.override = true
                options.nbtData = nbtData
                summonMobs(options)
                lmPlaceHolder.free()
            }
            "at-location" -> {
                var xStr = ""
                var yStr = ""
                var zStr = ""
                var world: World? = null
                for (i in 0..<miscArgs.size){
                    val arg = miscArgs[i]
                    if (arg.startsWith("{")) continue
                    when (i){
                        0 -> { xStr = arg }
                        1 -> { yStr = arg }
                        2 -> { zStr = arg }
                        3 -> {
                            world = Bukkit.getWorld(arg)
                            if (world == null){
                                MessagesHelper.showMessage(sender,
                            "command.levelledmobs.summon.atLocation.invalid-world","%world%", arg)
                                return
                            }
                        }
                    }
                }
                if (world == null){
                    MessagesHelper.showMessage(sender,
                        "command.levelledmobs.summon.atLocation.usage-console")
                    return
                }

                location = getRelativeLocation(sender, xStr, yStr, zStr, world)
                if (location == null){
                    sender.sendMessage("Invalid location")
                    return
                }

                val lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                    entityType, location
                )
                val options = SummonMobOptions(lmPlaceHolder, sender)
                options.amount = amount
                options.requestedLevel = requestedLevel
                options.summonType = SummonType.AT_LOCATION
                options.override = true
                options.nbtData = nbtData
                summonMobs(options)
                lmPlaceHolder.free()
            }
            "at-player" -> {
                var didSummon = false
                for (i in 0..<miscArgs.size){
                    val arg = miscArgs[i]
                    if (arg.startsWith("{")) continue
                    var offline = false
                    var world: World? = null

                    val target = Bukkit.getPlayer(arg)
                    if (target == null) {
                        offline = true
                    } else if (sender is Player) {
                        // Vanished player compatibility.
                        if (!sender.canSee(target) && !sender.isOp) {
                            offline = true
                        }
                        location = (target.location)
                        world = location.world
                    } else {
                        location = target.location
                        world = target.world
                    }

                    if (offline || world == null) {
                        MessagesHelper.showMessage(sender, "common.player-offline", "%player%", arg)
                        return
                    }

                    val lmPlaceHolder: LivingEntityPlaceholder = LivingEntityPlaceholder.getInstance(
                        entityType, location!!
                    )
                    val options = SummonMobOptions(lmPlaceHolder, sender)
                    options.amount = amount
                    options.requestedLevel = requestedLevel
                    options.summonType = SummonType.AT_PLAYER
                    options.player = target
                    options.override = true
                    options.nbtData = nbtData

                    summonMobs(options)
                    lmPlaceHolder.free()
                    didSummon = true
                    break
                }

                if (!didSummon){
                    MessagesHelper.showMessage(sender, "command.levelledmobs.summon.atPlayer.usage")
                }
            }
            else -> {
                MessagesHelper.showMessage(sender, "command.levelledmobs.summon.usage")
            }
        }
    }

    private fun buildTabSuggestions(
        info: SuggestionInfo<CommandSender>
    ): MutableList<String>{
        val args = Utils.splitStringWithQuotes(info.currentInput)

        // 0  1      2  3      4 5           6   7   8   9
        // lm summon 10 zombie 9 here
        //                       at-player   <player>
        //                       at-location <x> <y> <z> <world>

        when (args[5].lowercase()){
            "here" -> { return mutableListOf() }
            "at-player" -> {
                val suggestions = mutableListOf<String>()
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    if (info.sender is Player) {
                        val player = info.sender as Player
                        if (player.canSee(onlinePlayer) || player.isOp) {
                            suggestions.add(onlinePlayer.name)
                        }
                    } else {
                        suggestions.add(onlinePlayer.name)
                    }
                }
                return suggestions
            }
            "at-location" -> {
                if (args.size in 6..8) {
                    return Utils.oneToNine
                }
                else if (args.size == 9){
                    val worlds = mutableListOf<String>()
                    for (world in Bukkit.getWorlds()){
                        worlds.add(world.name)
                    }
                    return worlds
                }
                // TODO: add NBT suggestions
            }
            else -> { return mutableListOf() }
        }

        return mutableListOf()
    }

    enum class SummonType {
        HERE,
        AT_PLAYER,
        AT_LOCATION
    }

    private fun summonMobs(
        options: SummonMobOptions
    ) {
        val sender = options.sender
        val main = LevelledMobs.instance
        val target = options.player
        var location = options.lmPlaceholder.location

        if (main.levelManager.forcedBlockedEntityTypes.contains(
                options.lmPlaceholder.entityType
            )
        ) {
            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.not-levellable"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.replaceAllInList(
                messages, "%entity%",
                options.lmPlaceholder.typeName
            )
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
            return
        }

        if (!sender.isOp && !options.override
            && (main.levelInterface.getLevellableState(options.lmPlaceholder)
                    !== LevellableState.ALLOWED)
        ) {
            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.not-levellable"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.replaceAllInList(
                messages, "%entity%",
                options.lmPlaceholder.typeName
            )
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
            return
        }

        if (options.amount < 1) {
            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.amount-limited.min"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        val maxAmount: Int = main.helperSettings.getInt(
            "customize-summon-command-limit", 100
        )
        if (options.amount > maxAmount) {
            options.amount = maxAmount

            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.amount-limited.max"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.replaceAllInList(messages, "%maxAmount%", maxAmount.toString())
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        val levels: MinAndMaxHolder = main.levelManager.getMinAndMaxLevels(options.lmPlaceholder)

        if (options.requestedLevel!!.levelMin < levels.min && !sender.hasPermission(
                "levelledmobs.command.summon.bypass-level-limit"
            ) && !options.override
        ) {
            options.requestedLevel!!.setMinAllowedLevel(levels.min)

            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.level-limited.min"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.replaceAllInList(messages, "%minLevel%", levels.min.toString())
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        if (options.requestedLevel!!.levelMax > levels.max && !sender.hasPermission(
                "levelledmobs.command.summon.bypass-level-limit"
            ) && !options.override
        ) {
            options.requestedLevel!!.setMaxAllowedLevel(levels.max)

            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.level-limited.max"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.prefix)
            messages = Utils.replaceAllInList(messages, "%maxLevel%", levels.max.toString())
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        if (options.summonType === SummonType.HERE) {
            location = addVarianceToLocation(location)
        }

        if (options.summonType === SummonType.HERE || options.summonType === SummonType.AT_PLAYER) {
            val distFromPlayer = main.helperSettings.getInt(
                "summon-command-spawn-distance-from-player", 5
            )
            val minDistFromPlayer = main.helperSettings.getInt(
                "summon-command-spawn-min-distance-from-player", 0
            )
            if (distFromPlayer > 0 && target != null) {
                val locationTemp = getSpawnLocation(target, location, options.lmPlaceholder.entityType!!)
                if (locationTemp == null) {
                    sender.sendMessage("Unable to find a suitable spawn location")
                    return
                }
                location = locationTemp
            } else if (target == null && sender is BlockCommandSender) {
                // increase the y by one so they don't spawn inside the command block
                location = Location(
                    location.world, location.blockX.toDouble(),
                    (location.blockY + 2).toDouble(), location.blockZ.toDouble()
                )
            }
        }

        for (i in 0 until options.amount) {
            assert(location.world != null)
            val useLevel = if (options.requestedLevel!!.hasLevelRange) ThreadLocalRandom.current().nextInt(
                options.requestedLevel!!.levelRangeMin,
                options.requestedLevel!!.levelRangeMax + 1
            ) else options.requestedLevel!!.level

            val entity = location.world
                .spawnEntity(location, options.lmPlaceholder.entityType!!)

            if (entity is LivingEntity) {
                val lmEntity = LivingEntityWrapper.getInstance(entity)
                lmEntity.summonedLevel = useLevel
                lmEntity.isNewlySpawned = true
                synchronized(LevelManager.summonedOrSpawnEggs_Lock) {
                    main.levelManager.summonedOrSpawnEggs.put(lmEntity.livingEntity, null)
                }
                if (!options.nbtData.isNullOrEmpty()) {
                    lmEntity.nbtData = mutableListOf(options.nbtData!!)
                }
                lmEntity.summonedSender = sender
                main.levelInterface.applyLevelToMob(
                    lmEntity, useLevel, true, options.override,
                    mutableSetOf(AdditionalLevelInformation.NOT_APPLICABLE)
                )
                synchronized(lmEntity.livingEntity.persistentDataContainer) {
                    lmEntity.pdc
                        .set(NamespacedKeys.wasSummoned, PersistentDataType.INTEGER, 1)
                }
                lmEntity.free()
            }
        }

        val printResults: Boolean = main.helperSettings.getBoolean("print-lm-summon-results", true)

        when (options.summonType) {
            SummonType.HERE -> {
                if (printResults) {
                    MessagesHelper.showMessage(sender,
                        "command.levelledmobs.summon.here.success",
                        arrayOf("%amount%", "%level%", "%entity%"),
                        arrayOf(
                            options.amount.toString(), options.requestedLevel.toString(),
                            options.lmPlaceholder.typeName
                        )
                    )
                }
            }

            SummonType.AT_LOCATION -> {
                if (printResults) {
                    MessagesHelper.showMessage(sender,
                        "command.levelledmobs.summon.atLocation.success",
                        arrayOf("%amount%", "%level%", "%entity%", "%x%", "%y%", "%z%", "%world%"),
                        arrayOf(
                            options.amount.toString(),
                            options.requestedLevel.toString(),
                            options.lmPlaceholder.typeName,
                            location.blockX.toString(),
                            location.blockY.toString(),
                            location.blockZ.toString(),
                            if (location.world == null) "(null)" else location.world.name
                        )
                    )
                }
            }

            SummonType.AT_PLAYER -> {
                if (printResults) {
                    val playerName: String =
                        if (main.ver.isRunningPaper) PaperUtils.getPlayerDisplayName(target)
                        else SpigotUtils.getPlayerDisplayName(target)
                    MessagesHelper.showMessage(sender,
                        "command.levelledmobs.summon.atPlayer.success",
                        arrayOf(
                            "%amount%", "%level%", "%entity%", "%targetUsername%",
                            "%targetDisplayname%"
                        ),
                        arrayOf(
                            options.amount.toString(),
                            options.requestedLevel.toString(),
                            options.lmPlaceholder.typeName,
                            target?.name ?: "(null)",
                            if (target == null) "(null)" else playerName
                        )
                    )
                }
            }

            else -> throw IllegalStateException(
                ("Unexpected SummonType value of " + options.summonType) + "!"
            )
        }
    }

    private fun getSpawnLocation(
        player: Player,
        location: Location,
        entityType: EntityType
    ): Location? {
        if (location.world == null) return null

        val main = LevelledMobs.instance
        var maxDistFromPlayer: Int? = main.helperSettings.getInt2(
            "summon-command-spawn-max-distance-from-player", null
        )

        if (maxDistFromPlayer == null) {
            // legacy name
            maxDistFromPlayer = main.helperSettings.getInt2(
                "summon-command-spawn-distance-from-player", null
            )
        }

        if (maxDistFromPlayer == null) maxDistFromPlayer = 5

        val minDistFromPlayer = min(
            main.helperSettings.getInt(
                "summon-command-spawn-min-distance-from-player", 3
            ).toDouble(), maxDistFromPlayer.toDouble()
        ).toInt()

        val blockCandidates = mutableListOf<Block>()
        val blocksNeeded = if (entityType == EntityType.ENDERMAN || entityType == EntityType.RAVAGER) 3
        else 2

        for (i in 0..9) {
            val useDistance: Int = if (minDistFromPlayer != maxDistFromPlayer) ThreadLocalRandom.current()
                .nextInt(minDistFromPlayer, maxDistFromPlayer) else maxDistFromPlayer
            val startingLocation: Location = getLocationNearPlayer(player, location, useDistance)
            var tempLocation = startingLocation.clone()
            var foundBlock = false
            val maxYVariance = max((maxDistFromPlayer - useDistance).toDouble(), 10.0).toInt()

            for (y in 0 until maxYVariance) {
                val block = tempLocation.add(0.0, min(y.toDouble(), 1.0), 0.0).block
                // start at player level and keep going up until a solid block is found
                if (block.type.isSolid) {
                    blockCandidates.add(block)
                    foundBlock = true
                    break
                }
            }

            if (!foundBlock) {
                tempLocation = startingLocation.clone()
                for (y in 1..maxYVariance) {
                    val block = tempLocation.add(0.0, -1.0, 0.0).block
                    // start at player level and keep going down until a solid block is found
                    if (block.type.isSolid) {
                        blockCandidates.add(block)
                        break
                    }
                }
            }

            if (blockCandidates.size >= 10) break
        }

        if (blockCandidates.isEmpty()) return null

        blockCandidates.shuffle()

        // return first block from the candiates that has 2 air spaces above it
        for (block in blockCandidates) {
            var notGoodSpot = false
            for (i in 1 until blocksNeeded + 1) {
                val temp = location.world.getBlockAt(block.x, block.y + i, block.z)
                if (!temp.isPassable) {
                    notGoodSpot = true
                    break
                }
            }

            if (notGoodSpot) continue

            return block.location.add(0.5, (blocksNeeded - 1).toDouble(), 0.5)
        }

        return null
    }

    private fun getLocationNearPlayer(
        player: Player,
        location: Location,
        useDistFromPlayer: Int
    ): Location {
        var newX = location.blockX
        var newZ = location.blockZ

        var rotation = ((player.location.yaw - 180) % 360).toDouble()
        if (rotation < 0) {
            rotation += 360.0
        }

        if (0 <= rotation && rotation < 22.5) // N
        {
            newZ -= useDistFromPlayer
        } else if (22.5 <= rotation && rotation < 67.5) { // NE
            newX += useDistFromPlayer
            newZ -= useDistFromPlayer
        } else if (67.5 <= rotation && rotation < 112.5) // E
        {
            newX += useDistFromPlayer
        } else if (112.5 <= rotation && rotation < 157.5) { // SE
            newX += useDistFromPlayer
            newZ += useDistFromPlayer
        } else if (157.5 <= rotation && rotation < 202.5) // S
        {
            newZ += useDistFromPlayer
        } else if (202.5 <= rotation && rotation < 247.5) { // SW
            newX -= useDistFromPlayer
            newZ += useDistFromPlayer
        } else if (247.5 <= rotation && rotation < 292.5) // W
        {
            newX -= useDistFromPlayer
        } else if (292.5 <= rotation && rotation < 337.5) { // NW
            newX -= useDistFromPlayer
            newZ -= useDistFromPlayer
        } else  // N
        {
            newZ -= useDistFromPlayer
        }

        return Location(location.world, newX.toDouble(), location.blockY.toDouble(), newZ.toDouble())
    }

    private fun getRelativeLocation(
        sender: CommandSender,
        xStr: String,
        yStr: String,
        zStr: String,
        world: World
    ): Location? {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var xRelative = false
        var yRelative = false
        var zRelative = false

        if (sender is Player || sender is BlockCommandSender) {
            //Player or Command blocks
            if (xStr[0] == '~') {
                x = if (sender is Player) {
                    sender.location.x
                } else {
                    (sender as BlockCommandSender).block.x.toDouble()
                }

                if (xStr.length > 1) {
                    val addition: Double
                    try {
                        addition = xStr.substring(1).toDouble()
                    } catch (ex: NumberFormatException) {
                        return null
                    }
                    x += addition
                }

                xRelative = true
            }
            if (yStr[0] == '~') {
                y = if (sender is Player) {
                    sender.location.y
                } else {
                    (sender as BlockCommandSender).block.y.toDouble()
                }

                if (yStr.length > 1) {
                    val addition: Double
                    try {
                        addition = yStr.substring(1).toDouble()
                    } catch (ex: NumberFormatException) {
                        return null
                    }

                    y += addition
                }

                yRelative = true
            }
            if (zStr[0] == '~') {
                z = if (sender is Player) {
                    sender.location.z
                } else {
                    (sender as BlockCommandSender).block.z.toDouble()
                }

                if (zStr.length > 1) {
                    val addition: Double
                    try {
                        addition = zStr.substring(1).toDouble()
                    } catch (ex: NumberFormatException) {
                        return null
                    }
                    z += addition
                }

                zRelative = true
            }
        }

        if (!xRelative) {
            try {
                x = xStr.toDouble()
            } catch (ex: NumberFormatException) {
                return null
            }
        }
        if (!yRelative) {
            try {
                y = yStr.toDouble()
            } catch (ex: NumberFormatException) {
                return null
            }
        }
        if (!zRelative) {
            try {
                z = zStr.toDouble()
            } catch (ex: NumberFormatException) {
                return null
            }
        }

        return Location(world, x, y, z)
    }

    private fun addVarianceToLocation(
        oldLocation: Location
    ): Location {
        val min = 0.5
        val max = 2.5

        //Creates 3x new Random()s for a different seed each time
        val random1 = Random()
        val random2 = Random()

        for (i in 0..19) {
            val x = oldLocation.x + min + (max - min) * random1.nextDouble()
            val z = oldLocation.z + min + (max - min) * random2.nextDouble()

            val newLocation = Location(
                oldLocation.world, x, oldLocation.y,
                z
            )
            if (newLocation.block.isPassable && newLocation.add(0.0, 1.0, 0.0).block
                    .isPassable
            ) {
                return newLocation
            }
        }

        return oldLocation
    }

    private fun getEntityNames(): MutableList<String>{
        val entityNames = mutableListOf<String>()

        for (entityType in EntityType.entries) {
            entityNames.add(entityType.toString().lowercase(Locale.getDefault()))
        }
        return entityNames
    }
}