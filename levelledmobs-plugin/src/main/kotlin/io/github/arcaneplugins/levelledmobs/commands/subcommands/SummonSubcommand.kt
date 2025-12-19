package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.misc.LivingEntityPlaceholder
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.Locale
import java.util.Random
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
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
object SummonSubcommand : CommandBase("levelledmobs.command.summon"){
    override val description = "Various commands for summoning mobs."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack> {
        return createLiteralCommand("summon")
            .executes { ctx -> processCmd(ctx)
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createNumberArgument("number")
                .executes { ctx -> processCmd(ctx)
                    return@executes Command.SINGLE_SUCCESS }
                .then(createStringArgument("mobtype")
                    .suggests { _, builder -> getEntityNames(builder) }
                    .executes { ctx -> processCmd(ctx)
                        return@executes Command.SINGLE_SUCCESS
                    }
                    .then(createNumberArgument("moblevel")
                        .executes { ctx -> processCmd(ctx)
                            return@executes Command.SINGLE_SUCCESS
                        }
                        .then(createStringArgument("designator")
                            .suggests { _, builder -> builder.suggest("here").suggest("at-player")
                                .suggest("at-location").buildFuture() }
                            .executes { ctx -> processCmd(ctx)
                                return@executes Command.SINGLE_SUCCESS
                            }
                            .then(createGreedyStringArgument("values")
                                .suggests { ctx, builder -> buildTabSuggestions(ctx, builder) }
                                .executes { ctx -> processCmd(ctx)
                                    return@executes Command.SINGLE_SUCCESS
                                })))))
            .build()
    }

    private fun processCmd(
        ctx: CommandContext<CommandSourceStack>
    ){
        val sender = ctx.source.sender
        commandSender = sender
        val amount = getIntegerArgument(ctx, "number")
        val mobLevel = getIntegerArgument(ctx, "moblevel")
        val mobType = getStringArgument(ctx, "mobtype")
        var designator = getStringArgument(ctx, "designator")
        val values = getStringArgument(ctx, "values")

        if (designator.isEmpty()) designator = "here"

        if (amount == null || mobLevel == null || mobType.isEmpty()) {
            showMessage("command.levelledmobs.summon.usage")
            return
        }

        val entityType: EntityType
        try {
            entityType = EntityType.valueOf(mobType.uppercase())
        } catch (_: IllegalArgumentException) {
            showMessage("command.levelledmobs.summon.invalid-entity-type", "%entityType%", mobType)
            return
        }

        val requestedLevel = RequestedLevel()
        requestedLevel.level = mobLevel
        var location = when (sender) {
            is Player -> sender.location
            is BlockCommandSender -> sender.block.location
            else -> null
        }
        var nbtData: String? = null

        val miscArgs = splitStringWithQuotes(values, false)
        // 0           1   2   3   4
        // <nbtdata>
        // at-player   <player>
        // at-location <x> <y> <z> <world>
        for (i in 2..<miscArgs.size){
            val arg = miscArgs[i]
            if (arg.startsWith('{') && arg.endsWith('}'))
                nbtData = arg
        }

        when (designator.lowercase()){
            "here" -> {
                if (location == null){
                    showMessage("command.levelledmobs.summon.here.usage")
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
                    if (arg.startsWith('{')) continue
                    when (i){
                        0 -> { xStr = arg }
                        1 -> { yStr = arg }
                        2 -> { zStr = arg }
                        3 -> {
                            world = Bukkit.getWorld(arg)
                            if (world == null){
                                showMessage("command.levelledmobs.summon.atLocation.invalid-world",
                                    "%world%",
                                    arg
                                )
                                return
                            }
                        }
                    }
                }
                if (world == null){
                    showMessage("command.levelledmobs.summon.atLocation.usage-console")
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
                        if (!sender.canSee(target) && !sender.isOp)
                            offline = true

                        location = (target.location)
                        world = location.world
                    } else {
                        location = target.location
                        world = target.world
                    }

                    if (offline || world == null) {
                        showMessage("common.player-offline", "%player%", arg)
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

                if (!didSummon)
                    showMessage("command.levelledmobs.summon.atPlayer.usage")
            }
            else -> showMessage("command.levelledmobs.summon.usage")
        }
    }

    private fun buildTabSuggestions(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions>{
        val args = splitStringWithQuotes(ctx.input, true)
        val prefix = StringBuilder()
        val existingItems = mutableListOf<String>()

        for (i in 6..<args.size){
            existingItems.add(args[i].lowercase())
            prefix.append(args[i]).append(' ')
        }

        // 0  1      2  3      4 5           6   7   8   9
        // lm summon 10 zombie 9 here
        //                       at-player   <player>
        //                       at-location <x> <y> <z> <world>

        when (args[5].lowercase()){
            "here" -> { return builder.buildFuture() }
            "at-player" -> {
                for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                    if (ctx.source.sender is Player) {
                        val player = ctx.source.sender as Player
                        if (player.canSee(onlinePlayer) || player.isOp)
                            builder.suggest(onlinePlayer.name)
                    }
                    else
                        builder.suggest(onlinePlayer.name)
                }
                return builder.buildFuture()
            }
            "at-location" -> {
                if (args.size in 6..8)
                    Utils.getOneToNineSuggestions(builder)
                else if (args.size == 9){
                    for (world in Bukkit.getWorlds()){
                        builder.suggest("$prefix${world.name}")
                    }
                    return builder.buildFuture()
                }
                // TODO: add NBT suggestions
            }
        }

        return builder.buildFuture()
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
            showMessage("command.levelledmobs.summon.not-levellable")
            return
        }

        if (!sender.isOp && !options.override
            && (main.levelInterface.getLevellableState(options.lmPlaceholder)
                    !== LevellableState.ALLOWED)
        ) {
            showMessage("command.levelledmobs.summon.not-levellable")
            return
        }

        if (options.amount < 1)
            showMessage("command.levelledmobs.summon.amount-limited.min")

        val maxAmount = main.helperSettings.getInt(
            "customize-summon-command-limit", 100
        )
        if (options.amount > maxAmount) {
            options.amount = maxAmount
            showMessage("command.levelledmobs.summon.amount-limited.max")
        }

        val levels = main.levelManager.getMinAndMaxLevels(options.lmPlaceholder)

        if (options.requestedLevel!!.levelMin < levels.min && !sender.hasPermission(
                "levelledmobs.command.summon.bypass-level-limit"
            ) && !options.override
        ) {
            options.requestedLevel!!.setMinAllowedLevel(levels.minAsInt)
            showMessage("command.levelledmobs.summon.level-limited.min")
        }

        if (options.requestedLevel!!.levelMax > levels.max && !sender.hasPermission(
                "levelledmobs.command.summon.bypass-level-limit"
            ) && !options.override
        ) {
            options.requestedLevel!!.setMaxAllowedLevel(levels.maxAsInt)
            showMessage("command.levelledmobs.summon.level-limited.max")
        }

        if (options.summonType === SummonType.HERE)
            location = addVarianceToLocation(location)

        if (options.summonType === SummonType.HERE || options.summonType === SummonType.AT_PLAYER) {
            val distFromPlayer = main.helperSettings.getInt(
                "summon-command-spawn-distance-from-player", 5
            )
            if (distFromPlayer > 0 && target != null) {
                // val locationTemp = getSpawnLocation(target, location, options.lmPlaceholder.entityType!!)
                val locationTemp = getSpawnLocation(location, options)
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

        val future = CompletableFuture<Boolean>()
        val wrapper = SchedulerWrapper(options.player){
            spawnMobs(sender, options, location)
            future.complete(true)
        }
        wrapper.runDirectlyInBukkit = true
        wrapper.runDirectlyInFolia = sender is Player
        wrapper.locationForRegionScheduler = location
        wrapper.run()

        future.get(100L, TimeUnit.MILLISECONDS)

        val printResults = main.helperSettings.getBoolean("print-lm-summon-results", true)

        when (options.summonType) {
            SummonType.HERE -> {
                if (printResults) {
                    showMessage("command.levelledmobs.summon.here.success",
                        mutableListOf("%amount%", "%level%", "%entity%"),
                        mutableListOf(
                            options.amount.toString(), options.requestedLevel.toString(),
                            options.lmPlaceholder.typeName
                        )
                    )
                }
            }

            SummonType.AT_LOCATION -> {
                if (printResults) {
                    showMessage("command.levelledmobs.summon.atLocation.success",
                        mutableListOf("%amount%", "%level%", "%entity%", "%x%", "%y%", "%z%", "%world%"),
                        mutableListOf(
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
                        mutableListOf(
                            "%amount%", "%level%", "%entity%", "%targetUsername%",
                            "%targetDisplayname%"
                        ),
                        mutableListOf(
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

    private fun spawnMobs(
        sender: CommandSender,
        options: SummonMobOptions,
        location: Location
    ){
        val main = LevelledMobs.instance
        val requestedLevel = options.requestedLevel!!

        repeat(options.amount) {
            assert(location.world != null)

            val useLevel = if (requestedLevel.hasLevelRange) ThreadLocalRandom.current().nextInt(
                requestedLevel.levelRangeMin,
                requestedLevel.levelRangeMax + 1
            ) else requestedLevel.level

            val entity = location.world
                .spawnEntity(location, options.lmPlaceholder.entityType!!)

            if (entity is LivingEntity) {
                val lmEntity = LivingEntityWrapper.getInstance(entity)
                lmEntity.summonedLevel = useLevel
                lmEntity.isNewlySpawned = true
                synchronized(LevelManager.summonedOrSpawnEggs_Lock) {
                    main.levelManager.summonedOrSpawnEggs.put(lmEntity.livingEntity, null)
                }
                if (!options.nbtData.isNullOrEmpty())
                    lmEntity.nbtData = mutableListOf(options.nbtData!!)

                lmEntity.summonedSender = sender
                synchronized(lmEntity.livingEntity.persistentDataContainer) {
                    lmEntity.pdc
                        .set(NamespacedKeys.wasSummoned, PersistentDataType.INTEGER, 1)
                }
                MobDataManager.populateAttributeCache(lmEntity)
                main.levelInterface.applyLevelToMob(
                    lmEntity, useLevel, true, options.override,
                    mutableSetOf(AdditionalLevelInformation.NOT_APPLICABLE)
                )
                lmEntity.free()
            }
        }
    }

    private fun getSpawnLocation(
        location: Location,
        options: SummonMobOptions
    ): Location? {
        if (location.world == null) return null
        val entityType = options.lmPlaceholder.entityType!!

        val main = LevelledMobs.instance
        var maxDistFromPlayer = main.helperSettings.getInt2(
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

        val blocksNeeded = if (entityType == EntityType.ENDERMAN || entityType == EntityType.RAVAGER) 3
        else 2

        val completableFuture = CompletableFuture<MutableList<Block>>()
        val target = options.player!!
        var wrapper = SchedulerWrapper(target){
            val blockCandidates = getBlockCandidates(
                minDistFromPlayer, maxDistFromPlayer, target, location
            )
            completableFuture.complete(blockCandidates)
        }
        wrapper.runDirectlyInBukkit = true
        if (options.sender is Player) wrapper.runDirectlyInFolia = true
        wrapper.run()
        val blockCandidates = completableFuture.get(100L, TimeUnit.MILLISECONDS)
        if (blockCandidates.isEmpty()) return null

        blockCandidates.shuffle()

        val futureCandidate = CompletableFuture<Location?>()
        wrapper = SchedulerWrapper(target){
            val foundLocation = checkBlockCandidates(blockCandidates, blocksNeeded, location.world)
            futureCandidate.complete(foundLocation)
        }
        wrapper.runDirectlyInBukkit = true
        if (options.sender is Player) wrapper.runDirectlyInFolia = true
        wrapper.run()

        val foundLocation = futureCandidate.get(500L, TimeUnit.MILLISECONDS)
        return foundLocation
    }

    private fun getBlockCandidates(
        minDistFromPlayer: Int,
        maxDistFromPlayer: Int,
        target: Player,
        location: Location
    ): MutableList<Block>{
        val blockCandidates = mutableListOf<Block>()

        @Suppress("UNUSED_PARAMETER")
        for (i in 0..9) {
            val useDistance = if (minDistFromPlayer != maxDistFromPlayer) ThreadLocalRandom.current()
                .nextInt(minDistFromPlayer, maxDistFromPlayer) else maxDistFromPlayer
            val startingLocation = getLocationNearPlayer(target, location, useDistance)
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

        return blockCandidates
    }

    private fun checkBlockCandidates(
        blockCandidates: MutableList<Block>,
        blocksNeeded: Int,
        world: World
    ): Location? {
        // return first block from the candiates that has 2 air spaces above it
        for (block in blockCandidates) {
            var notGoodSpot = false
            for (i in 1 until blocksNeeded + 1) {
                val temp = world.getBlockAt(block.x, block.y + i, block.z)
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
        if (rotation < 0) rotation += 360.0

        when (rotation) {
            in 0.0..<22.5 // N
                -> {
                newZ -= useDistFromPlayer
            }

            in 22.5..<67.5 -> { // NE
                newX += useDistFromPlayer
                newZ -= useDistFromPlayer
            }

            in 67.5..<112.5 // E
                -> {
                newX += useDistFromPlayer
            }

            in 112.5..<157.5 -> { // SE
                newX += useDistFromPlayer
                newZ += useDistFromPlayer
            }

            in 157.5..<202.5 // S
                -> {
                newZ += useDistFromPlayer
            }

            in 202.5..<247.5 -> { // SW
                newX -= useDistFromPlayer
                newZ += useDistFromPlayer
            }

            in 247.5..<292.5 // W
                -> {
                newX -= useDistFromPlayer
            }

            in 292.5..<337.5 -> { // NW
                newX -= useDistFromPlayer
                newZ -= useDistFromPlayer
            }

            else  // N
                -> {
                newZ -= useDistFromPlayer
            }
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
        val x = processRelativeCoords(sender, xStr, CoordType.X) ?: return null
        val y = processRelativeCoords(sender, yStr, CoordType.Y) ?: return null
        val z = processRelativeCoords(sender, zStr, CoordType.Z) ?: return null

        return Location(world, x, y, z)
    }

    private fun processRelativeCoords(
        sender: CommandSender,
        input: String,
        coordType: CoordType
    ): Double? {
        if (input.isEmpty()) return null

        if (input[0] != '~') {
            return try { input.toDouble() }
            catch (_: NumberFormatException) { null }
        }

        var result = if (sender is Player) {
            when (coordType) {
                CoordType.X -> sender.location.z
                CoordType.Y -> sender.location.y
                CoordType.Z -> sender.location.z
            }
        }
        else {
            when (coordType) {
                CoordType.X -> (sender as BlockCommandSender).block.x.toDouble()
                CoordType.Y -> (sender as BlockCommandSender).block.y.toDouble()
                CoordType.Z -> (sender as BlockCommandSender).block.z.toDouble()
            }
        }

        if (input.length > 1) {
            val addition: Double
            try {
                addition = input.substring(1).toDouble()
            } catch (_: NumberFormatException) {
                return null
            }
            result += addition
        }

        return result
    }

    private fun addVarianceToLocation(
        oldLocation: Location
    ): Location {
        val min = 0.5
        val max = 2.5

        //Creates 3x new Random()s for a different seed each time
        val random1 = Random()
        val random2 = Random()

        repeat(20) {
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

    private fun getEntityNames(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions>{
        for (entityType in EntityType.entries) {
            builder.suggest(entityType.toString().lowercase(Locale.getDefault()))
        }

        return builder.buildFuture()
    }

    private enum class CoordType{
        X, Y, Z
    }
}