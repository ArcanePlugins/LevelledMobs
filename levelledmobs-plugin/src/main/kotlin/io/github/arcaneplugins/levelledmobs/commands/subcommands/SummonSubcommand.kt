package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.util.LinkedList
import java.util.Locale
import java.util.Random
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.misc.LivingEntityPlaceholder
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
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
class SummonSubcommand : MessagesBase(), Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        commandSender = sender
        messageLabel = label

        if (!sender.hasPermission("levelledmobs.command.summon")) {
            LevelledMobs.instance.configUtils.sendNoPermissionMsg(sender)
            return
        }

        var useOverride = false
        val useArgs: MutableList<String> = LinkedList()
        var startOfNbt = -1
        var endOfNbt = -1

        for (i in args.indices) {
            val arg = args[i]
            if ("/override".equals(arg, ignoreCase = true)) {
                useOverride = true
            } else {
                useArgs.add(arg)
            }

            if (startOfNbt == -1 && arg.startsWith("{")) {
                startOfNbt = i
                if (arg.endsWith("}")) {
                    endOfNbt = i
                }
            } else if (startOfNbt >= 0 && endOfNbt == -1 && arg.endsWith("}")) {
                endOfNbt = i
            }
        }

        var nbtData: String? = null
        if (startOfNbt >= 0 && endOfNbt >= 0 && endOfNbt >= startOfNbt) {
            nbtData = useArgs.subList(startOfNbt, endOfNbt + 1).toString()
            nbtData = nbtData.substring(1, nbtData.length - 1)
            useArgs.subList(startOfNbt, endOfNbt + 1).clear()
        }

        parseSubcommand2(useArgs.toMutableList(), useOverride, nbtData)
    }

    private fun parseSubcommand2(
        args: MutableList<String>,
        override: Boolean,
        nbtData: String?
    ) {
        if (args.size < 4) {
            showMessage("command.levelledmobs.summon.usage")
            return
        }

        val amount: Int
        try {
            amount = args[1].toInt()
        } catch (ex: NumberFormatException) {
            showMessage("command.levelledmobs.summon.invalid-amount", "%amount%", args[1])
            // messages = Utils.replaceAllInList(messages, "%amount%", args[1]); // This is after colorize so that args[1] is not colorized.
            return
        }

        val entityType: EntityType
        try {
            entityType = EntityType.valueOf(args[2].uppercase(Locale.getDefault()))
        } catch (ex: IllegalArgumentException) {
            showMessage("command.levelledmobs.summon.invalid-entity-type", "%entityType%", args[2])
            // messages = Utils.replaceAllInList(messages, "%entityType%", args[2]); // This is after colorize so that args[2] is not colorized.
            return
        }

        val requestedLevel = RequestedLevel()
        if (!requestedLevel.setLevelFromString(args[3])) {
            showMessage("command.levelledmobs.summon.invalid-level", "%level%", args[3])
            //messages = Utils.replaceAllInList(messages, "%level%", args[3]); // This is after colorize so that args[3] is not colorized.
            return
        }

        var summonType: SummonType = SummonType.HERE
        if (args.size > 4) {
            when (args[4].lowercase(Locale.getDefault())) {
                "here" -> {
                }

                "atplayer" -> summonType = SummonType.AT_PLAYER
                "atlocation" -> summonType = SummonType.AT_LOCATION
                else -> {
                    showMessage(
                        "command.levelledmobs.summon.invalid-level", "%summonType%",
                        args[4]
                    )
                    return
                }
            }
        }

        if (summonType == SummonType.HERE) {
            if (commandSender !is Player
                && commandSender !is BlockCommandSender
            ) {
                showMessage("command.levelledmobs.summon.invalid-summon-type-console")
                return
            }

            if (args.size == 4 || args.size == 5) {
                var player: Player? = null
                if (commandSender is Player) {
                    player = commandSender as Player?
                }
                val location = if ((player != null)) (commandSender as Player).location
                else (commandSender as BlockCommandSender).block.location

                if (location.world == null) {
                    val playerName: String =
                        if (LevelledMobs.instance.ver.isRunningPaper) PaperUtils.getPlayerDisplayName(player)
                        else SpigotUtils.getPlayerDisplayName(player)
                    showMessage("common.player-offline", "%player%", playerName)
                    return
                }

                val lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                    entityType, location
                )

                val options = SummonMobOptions(lmPlaceHolder, commandSender!!)
                options.amount = amount
                options.requestedLevel = requestedLevel
                options.summonType = summonType
                options.player = player
                options.override = override
                options.nbtData = nbtData

                summonMobs(options)
                lmPlaceHolder.free()
            } else {
                showMessage("command.levelledmobs.summon.here.usage")
            }
        } else if (summonType == SummonType.AT_PLAYER) {
            if (args.size == 6) {
                var offline = false
                var location: Location? = null
                var world: World? = null

                val target = Bukkit.getPlayer(args[5])
                if (target == null) {
                    offline = true
                } else if (commandSender is Player) {
                    // Vanished player compatibility.
                    if (!(commandSender as Player).canSee(target) && !(commandSender as Player).isOp) {
                        offline = true
                    }
                    location = (target.location)
                    world = location.world
                } else {
                    location = target.location
                    world = target.world
                }

                if (offline || world == null) {
                    showMessage("common.player-offline", "%player%", args[5])
                    // messages = Utils.replaceAllInList(messages, "%player%", args[5]); // This is after colorize so that args[5] is not colorized.
                    return
                }

                val lmPlaceHolder: LivingEntityPlaceholder = LivingEntityPlaceholder.getInstance(
                    entityType, location!!
                )
                val options = SummonMobOptions(lmPlaceHolder, commandSender!!)
                options.amount = amount
                options.requestedLevel = requestedLevel
                options.summonType = summonType
                options.player = target
                options.override = override
                options.nbtData = nbtData

                summonMobs(options)
                lmPlaceHolder.free()
            } else {
                showMessage("command.levelledmobs.summon.atPlayer.usage")
            }
        } else { // At Location
            if (args.size == 8 || args.size == 9) {
                val worldName = if (args.size == 8) {
                    when (commandSender) {
                        is Player -> {
                            (commandSender as Player).world.name
                        }

                        is BlockCommandSender -> {
                            (commandSender as BlockCommandSender).block.world
                                .name
                        }

                        else -> {
                            showMessage("command.levelledmobs.summon.atLocation.usage-console")
                            return
                        }
                    }
                } else { //args.length==9
                    val world = Bukkit.getWorld(args[8])

                    if (world == null) {
                        showMessage(
                            "command.levelledmobs.summon.atLocation.usage-console",
                            "%world%", args[8]
                        )
                        // messages = Utils.replaceAllInList(messages, "%world%", args[8]); //This is after colorize so that args[8] is not colorized.
                        return
                    } else {
                        world.name
                    }
                }

                val location = getRelativeLocation(
                    commandSender!!, args[5], args[6],
                    args[7], worldName
                )

                if (location?.world == null) {
                    showMessage("command.levelledmobs.summon.atLocation.invalid-location")
                } else {
                    val lmPlaceHolder = LivingEntityPlaceholder.getInstance(
                        entityType, location
                    )
                    val options = SummonMobOptions(
                        lmPlaceHolder,
                        commandSender!!
                    )
                    options.amount = amount
                    options.requestedLevel = requestedLevel
                    options.summonType = summonType
                    options.override = override
                    options.nbtData = nbtData
                    summonMobs(options)
                    lmPlaceHolder.free()
                }
            } else {
                showMessage("command.levelledmobs.summon.atLocation.usage")
            }
        }
    }

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String>? {
        if (!sender.hasPermission("levelledmobs.command.summon")) {
            return null
        }

        // len:    1      2        3        4       5          6            7   8     9     10
        // arg:    0      1        2        3       4          5            6   7     8     9
        // lvlmobs summon <amount> <entity> <level> here       /override
        // lvlmobs summon <amount> <entity> <level> atPlayer   <playername> /override
        // lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z> [world] /override

        // <amount>
        if (args.size == 2) {
            return Utils.oneToNine
        }

        // <entity>
        if (args.size == 3) {
            val entityNames: MutableList<String> = LinkedList()
            for (entityType in EntityType.entries) {
                entityNames.add(entityType.toString().lowercase(Locale.getDefault()))
            }
            return entityNames
        }

        // <level>
        if (args.size == 4) {
            return Utils.oneToNine
        }

        // here, atPlayer, atLocation
        if (args.size == 5) {
            return mutableListOf("here", "atPlayer", "atLocation", "/override")
        }

        var skipOverride = false
        for (i in 5 until args.size) {
            val arg = args[i]
            if (arg.startsWith("{") && !arg.endsWith("}")) {
                skipOverride = true
            }
            if (skipOverride && arg.endsWith("}")) {
                skipOverride = false
            }
        }
        if (args[args.size - 1].endsWith("}")) {
            skipOverride = true
        }

        // no suggestions for 'here' since it is the last argument for itself
        // these are for atPlayer and atLocation
        if (args.size > 5) {
            when (args[4].lowercase(Locale.getDefault())) {
                "atplayer" -> {
                    if (args.size == 6) {
                        val suggestions: MutableList<String> = LinkedList()
                        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                            if (sender is Player) {
                                if (sender.canSee(onlinePlayer) || sender.isOp()) {
                                    suggestions.add(onlinePlayer.name)
                                }
                            } else {
                                suggestions.add(onlinePlayer.name)
                            }
                        }
                        return suggestions
                    } else if (args.size == 7) {
                        return if (!skipOverride) {
                            mutableListOf("/override")
                        } else {
                            mutableListOf()
                        }
                    }
                }

                "atlocation" -> {
                    if (args.size < 9) { // args 6, 7 and 8 = x, y and z
                        return mutableListOf("~")
                    } else if (args.size == 9) {
                        val worlds: MutableList<String> = LinkedList()
                        Bukkit.getWorlds().forEach(Consumer { world: World ->
                            worlds.add(
                                world.name
                            )
                        })
                        return worlds
                    } else if (args.size == 10) {
                        return if (!skipOverride) {
                            mutableListOf("/override")
                        } else {
                            mutableListOf()
                        }
                    }
                }

                "here" -> {
                    return if (!skipOverride) {
                        mutableListOf("/override")
                    } else {
                        mutableListOf()
                    }
                }

                else -> {
                    return mutableListOf()
                }
            }
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
        val sender: CommandSender = options.sender
        val main = LevelledMobs.instance
        val target: Player? = options.player
        var location = options.lmPlaceholder.location

        if (main.levelManager.forcedBlockedEntityTypes.contains(
                options.lmPlaceholder.entityType
            )
        ) {
            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.not-levellable"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
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
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
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
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        val maxAmount: Int = main.helperSettings.getInt(
            main.settingsCfg,
            "customize-summon-command-limit", 100
        )
        if (options.amount > maxAmount) {
            options.amount = maxAmount

            var messages = main.messagesCfg.getStringList(
                "command.levelledmobs.summon.amount-limited.max"
            )
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
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
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
            messages = Utils.replaceAllInList(messages, "%minLevel%", java.lang.String.valueOf(levels.min))
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
            messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix())
            messages = Utils.replaceAllInList(messages, "%maxLevel%", java.lang.String.valueOf(levels.max))
            messages = Utils.colorizeAllInList(messages)
            messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
        }

        if (options.summonType === SummonType.HERE) {
            location = addVarianceToLocation(location)
        }

        if (options.summonType === SummonType.HERE || options.summonType === SummonType.AT_PLAYER) {
            val distFromPlayer = main.settingsCfg.getInt(
                "summon-command-spawn-distance-from-player", 5
            )
            val minDistFromPlayer = main.settingsCfg.getInt(
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

        val printResults: Boolean = main.helperSettings.getBoolean(main.settingsCfg, "print-lm-summon-results", true)

        when (options.summonType) {
            SummonType.HERE -> {
                if (printResults) {
                    showMessage(
                        "command.levelledmobs.summon.here.success",
                        arrayOf("%amount%", "%level%", "%entity%"),
                        arrayOf(
                            java.lang.String.valueOf(options.amount), options.requestedLevel.toString(),
                            options.lmPlaceholder.typeName
                        )
                    )
                }
            }

            SummonType.AT_LOCATION -> {
                if (printResults) {
                    showMessage(
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
                    showMessage(
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
            main.settingsCfg,
            "summon-command-spawn-max-distance-from-player", null
        )

        if (maxDistFromPlayer == null) {
            // legacy name
            maxDistFromPlayer = main.helperSettings.getInt2(
                main.settingsCfg,
                "summon-command-spawn-distance-from-player", null
            )
        }

        if (maxDistFromPlayer == null) maxDistFromPlayer = 5

        val minDistFromPlayer = min(
            main.settingsCfg.getInt(
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
        worldName: String
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
                    } catch (ex: java.lang.NumberFormatException) {
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
                    } catch (ex: java.lang.NumberFormatException) {
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
                    } catch (ex: java.lang.NumberFormatException) {
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
            } catch (ex: java.lang.NumberFormatException) {
                return null
            }
        }
        if (!yRelative) {
            try {
                y = yStr.toDouble()
            } catch (ex: java.lang.NumberFormatException) {
                return null
            }
        }
        if (!zRelative) {
            try {
                z = zStr.toDouble()
            } catch (ex: java.lang.NumberFormatException) {
                return null
            }
        }

        val world = Bukkit.getWorld(worldName) ?: return null

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
}