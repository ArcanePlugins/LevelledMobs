package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import java.util.SortedMap
import java.util.TreeMap
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import io.github.arcaneplugins.levelledmobs.annotations.DoNotShow
import io.github.arcaneplugins.levelledmobs.annotations.RuleFieldInfo
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper
import io.github.arcaneplugins.levelledmobs.enums.RuleType
import io.github.arcaneplugins.levelledmobs.listeners.EntitySpawnListener
import io.github.arcaneplugins.levelledmobs.misc.EffectiveInfo
import io.github.arcaneplugins.levelledmobs.rules.strategies.PlayerLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector

/**
 * Shows the current rules as parsed from the various config files
 *
 * @author stumper66
 * @since 3.0.0
 */
object RulesSubcommand : CommandBase("levelledmobs.command.rules") {
    override val description = "Used to view various rules."

    fun buildCommand() : LiteralCommandNode<CommandSourceStack>{
        return createLiteralCommand("rules")
            .executes { ctx ->
                commandSender = ctx.source.sender
                showMessage("command.levelledmobs.rules.incomplete-command")
                return@executes Command.SINGLE_SUCCESS
            }
            .then(createLiteralCommand("show-all")
                .then(createStringArgument("console")
                    .suggests { _, builder -> builder.suggest("console").buildFuture() }
                    .executes { ctx -> showAllRules(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx ->
                    showAllRules(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-rule")
                .then(createStringArgument("rulename")
                    .suggests { _, builder -> getAllRuleNames(builder) }
                    .executes { ctx -> showRule(ctx)
                        return@executes Command.SINGLE_SUCCESS })
                .executes { ctx -> showRule(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-effective")
                .then(createStringArgument("option1")
                    .suggests { ctx, builder -> buildShowEffectiveSuggestions(ctx, builder) }
                    .executes { ctx -> showEffectiveRules(ctx)
                        return@executes Command.SINGLE_SUCCESS }
                    .then(createStringArgument("option2")
                        .suggests { ctx, builder -> buildShowEffectiveSuggestions(ctx, builder) }
                        .executes { ctx -> showEffectiveRules(ctx)
                            return@executes Command.SINGLE_SUCCESS }))
                .executes { ctx -> showEffectiveRules(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("help-discord")
                .executes { ctx -> val message = MessagesHelper.getMessage("command.levelledmobs.rules.discord-invite")
                    showHyperlink(ctx.source.sender, message, "https://discord.gg/arcaneplugins-752310043214479462")
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("help-wiki")
                .executes { ctx -> val message = MessagesHelper.getMessage("command.levelledmobs.rules.wiki-link")
                    showHyperlink(ctx.source.sender, message, "https://arcaneplugins.gitbook.io/levelledmobs-the-ultimate-mob-levelling-solution/")
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("reset")
                .then(createStringArgument("difficulty")
                    .suggests { _, builder -> builder.suggest("vanilla").suggest("bronze").suggest("silver")
                        .suggest("gold").suggest("platinum").buildFuture() }
                    .executes { ctx -> resetRules(ctx)
                        return@executes Command.SINGLE_SUCCESS }
                    .then(createStringArgument("confirm")
                        .suggests { _, builder -> builder.suggest("confirm").buildFuture() }
                        .executes { ctx -> resetRules(ctx)
                            return@executes Command.SINGLE_SUCCESS }))
                .executes { ctx -> resetRules(ctx)
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("force-all")
                .executes { ctx -> commandSender = ctx.source.sender
                    forceRelevel()
                    return@executes Command.SINGLE_SUCCESS
                })
            .then(createLiteralCommand("show-temp-disabled")
                .executes { ctx -> showTempDisabled(ctx.source.sender)
                    return@executes Command.SINGLE_SUCCESS
                })
            .build()
    }

    private fun showAllRules(
        ctx: CommandContext<CommandSourceStack>
    ){
        val sender = ctx.source.sender
        commandSender = sender
        if (sender is Player)
            showMessage("command.levelledmobs.rules.console-rules")

        val showOnConsole = sender is ConsoleCommandSender || getStringArgumentAsBool(ctx, "console")
        val main = LevelledMobs.instance
        val sb = StringBuilder()

        for (rpi in main.rulesParsingManager.rulePresets.values) {
            sb.append(
                "\n&r--------------------------------- Preset rule ----------------------------------"
            ).append(rpi.formatRulesVisually(false, mutableListOf("ruleIsEnabled")))
        }

        sb.append(
            "\n&r--------------------------------- Default values -------------------------------"
        ).append(main.rulesParsingManager.defaultRule!!.formatRulesVisually())

        for (rpi in main.rulesParsingManager.customRules) {
            sb.append(
                "\n&r--------------------------------- Custom rule ----------------------------------"
            ).append(rpi.formatRulesVisually())
        }
        sb.append(
            "\n&r--------------------------------------------------------------------------------------"
        )

        if (showOnConsole)
            Log.inf(colorizeAll(sb.toString()))
        else
            sender.sendMessage(colorizeAll(sb.toString()))
    }

    private fun getAllRuleNames(
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            builder.suggest(ruleInfo.ruleName.replace(" ", "_"))
        }

        return builder.buildFuture()
    }

    private fun showTempDisabled(sender: CommandSender) {
        LevelledMobs.instance.rulesManager.showTempDisabledRules(sender)
    }

    private fun forceRelevel() {
        //TODO: make this work in Folia
        if (LevelledMobs.instance.ver.isRunningFolia) {
            commandSender!!.sendMessage("Sorry this command doesn't work in Folia")
            return
        }

        var worldCount = 0
        var entityCount = 0
        val main = LevelledMobs.instance

        main.reloadLM(commandSender!!)
        val isUsingPlayerLevelling = main.rulesManager.isPlayerLevellingEnabled()

        for (world in Bukkit.getWorlds()) {
            worldCount++
            for (entity in world.entities) {
                if (entity !is LivingEntity || entity is Player)
                    continue

                var doContinue = false
                synchronized(entity.persistentDataContainer) {
                    if (entity.persistentDataContainer.has(
                            NamespacedKeys.wasSummoned,
                            PersistentDataType.INTEGER
                        )
                    ) {
                        doContinue = true  // was summon using lm summon command.  don't relevel it
                    }
                }
                if (doContinue) continue

                entityCount++
                val lmEntity = LivingEntityWrapper.getInstance(entity)
                lmEntity.reEvaluateLevel = true
                lmEntity.isRulesForceAll = true
                lmEntity.wasPreviouslyLevelled = lmEntity.isLevelled
                if (isUsingPlayerLevelling)
                    EntitySpawnListener.updateMobForPlayerLevelling(lmEntity)

                main.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
                lmEntity.free()
            }
        }

        showMessage("command.levelledmobs.rules.rules-reprocessed",
            mutableListOf("%entitycount%", "%worldcount%"),
            mutableListOf(entityCount.toString(), worldCount.toString())
        )
    }

    private fun resetRules(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val sender = ctx.source.sender
        commandSender = sender
        val difficultyStr = getStringArgument(ctx, "difficulty")
        val confirm = getStringArgumentAsBool(ctx, "confirm")

        if (difficultyStr.isEmpty()) {
            showMessage("command.levelledmobs.rules.reset")
            return
        }

        val difficulty: ResetDifficulty = when (difficultyStr.lowercase()) {
            "vanilla" -> ResetDifficulty.VANILLA
            "bronze" -> ResetDifficulty.BRONZE
            "silver" -> ResetDifficulty.SILVER
            "gold" -> ResetDifficulty.GOLD
            "platinum" -> ResetDifficulty.PLATINUM
            else -> ResetDifficulty.UNSPECIFIED
        }

        if (difficulty == ResetDifficulty.UNSPECIFIED) {
            showMessage("command.levelledmobs.rules.invalid-difficulty", "%difficulty%", difficultyStr)
            return
        }

        if (!confirm) {
            showMessage("command.levelledmobs.rules.reset-syntax",
                "%difficulty%",
                difficultyStr
            )
            return
        }

        resetRules(sender, difficulty)
    }

    fun resetRules(
        sender: CommandSender?,
        difficulty: ResetDifficulty
    ) {
        val main = LevelledMobs.instance
        val prefix = main.configUtils.prefix
        if (sender != null) {
            MessagesHelper.showMessage(sender,
                "command.levelledmobs.rules.resetting", "%difficulty%",
                difficulty.toString()
            )
        }

        val filename = "rules.yml"
        val replaceWhat = arrayOf("    - challenge-silver", "")
        val replaceWith = arrayOf("    #- challenge-silver", "")

        when (difficulty) {
            ResetDifficulty.VANILLA -> {
                replaceWhat[1] = "#- challenge-vanilla"
                replaceWith[1] = "- challenge-vanilla"
            }

            ResetDifficulty.BRONZE -> {
                replaceWhat[1] = "#- challenge-bronze"
                replaceWith[1] = "- challenge-bronze"
            }

            ResetDifficulty.GOLD -> {
                replaceWhat[1] = "#- challenge-gold"
                replaceWith[1] = "- challenge-gold"
            }

            ResetDifficulty.PLATINUM -> {
                replaceWhat[1] = "#- challenge-platinum"
                replaceWith[1] = "- challenge-platinum"
            }

            ResetDifficulty.SILVER, ResetDifficulty.UNSPECIFIED -> {}
        }
        try {
            main.getResource(filename).use { stream ->
                if (stream == null) {
                    Log.sev("$prefix Input stream was null")
                    return
                }
                var rulesText =
                    String(stream.readAllBytes(), StandardCharsets.UTF_8)
                if (difficulty != ResetDifficulty.SILVER) {
                    rulesText = rulesText.replace(replaceWhat[0], replaceWith[0])
                        .replace(replaceWhat[1], replaceWith[1])
                }

                val rulesFile = File(main.dataFolder, filename)
                var rulesBackupFile = File(main.dataFolder, "rules.yml.backup")

                for (i in 0..9) {
                    if (!rulesBackupFile.exists())
                        break

                    rulesBackupFile = File(main.dataFolder, "rules.yml.backup$i")
                }

                Files.copy(
                    rulesFile.toPath(), rulesBackupFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                Files.writeString(rulesFile.toPath(), rulesText, StandardCharsets.UTF_8)
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        }

        if (sender != null) {
            MessagesHelper.showMessage(sender, "command.levelledmobs.rules.reset-complete")
            main.reloadLM(sender)
        }
    }

    enum class ResetDifficulty {
        VANILLA, BRONZE, SILVER, GOLD, PLATINUM, UNSPECIFIED
    }

    private fun showHyperlink(
        sender: CommandSender,
        message: String,
        url: String
    ) {
        if (sender !is Player) {
            sender.sendMessage(url)
            return
        }

        if (LevelledMobs.instance.ver.isRunningPaper)
            PaperUtils.sendHyperlink(sender, message, url)
        else
            SpigotUtils.sendHyperlink(sender, message, url)
    }

    private fun showRule(
        ctx: CommandContext<CommandSourceStack>
    ) {
        commandSender = ctx.source.sender
        val ruleName = getStringArgument(ctx, "rulename")
        val showOnConsole = commandSender!! is ConsoleCommandSender || getStringArgumentAsBool(ctx, "console")

        if (ruleName.isEmpty()) {
            showMessage("command.levelledmobs.rules.rule-name-missing")
            return
        }

        var rule: RuleInfo? = null
        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            val checkName = ruleInfo.ruleName.replace(" ", "_")
            if (ruleName.equals(checkName, ignoreCase = true)){
                rule = ruleInfo
                break
            }
        }

        if (rule == null) {
            showMessage("command.levelledmobs.rules.rule-name-invalid")
            return
        }

        val sb = StringBuilder()
        sb.append(
            MessagesHelper.getMessage(
                "command.levelledmobs.rules.showing-rules", "%rulename%",
                rule.ruleName
            )
        )

        sb.append(rule.formatRulesVisually(false, mutableListOf("id")))
        if (showOnConsole)
            Log.inf(sb.toString())
        else
            commandSender!!.sendMessage(colorizeAll(sb.toString()))
    }

    private fun showEffectiveRules(
        ctx: CommandContext<CommandSourceStack>
    ) {
        val sender = ctx.source.sender
        commandSender = ctx.source.sender
        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("Must be run by a player")
            return
        }

        var showOnConsole = false
        var findNearbyEntities = true

        for (option in getOptionalResults(ctx, mutableListOf("option1", "option2"))) {
            if ("console".equals(option, ignoreCase = true))
                showOnConsole = true
            else if ("looking-at".equals(option, ignoreCase = true))
                findNearbyEntities = false
        }

        val lmEntity: LivingEntityWrapper = getMobBeingLookedAt(player, findNearbyEntities, player)
            ?: return

        var entityName = lmEntity.typeName
        if (ExternalCompatibilityManager.hasMythicMobsInstalled
            && ExternalCompatibilityManager.isMythicMob(lmEntity)
        ) {
            entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
        }

        val locationStr =
            "${lmEntity.location.blockX}, ${lmEntity.location.blockY}, ${lmEntity.location.blockZ}"
        val mobLevel: String = if (lmEntity.isLevelled) lmEntity.getMobLevel.toString() else "0"
        val messages = MessagesHelper.getMessage(
            "command.levelledmobs.rules.effective-rules",
            mutableListOf("%mobname%", "%entitytype%", "%location%", "%world%", "%level%"),
            mutableListOf(
                entityName, lmEntity.nameIfBaby, locationStr, lmEntity.worldName,
                mobLevel
            )
        )

        val sb = StringBuilder()
        sb.append(
            messages.joinToString("\n").replace(
                LevelledMobs.instance.configUtils.prefix + " ", ""
            )
        )
        if (showOnConsole)
            Log.inf(sb.toString())
        else
            sender.sendMessage(sb.toString())

        if (!showOnConsole) sb.setLength(0)

        var mobHash: String? = null
        if (lmEntity.pdc.has(NamespacedKeys.mobHash, PersistentDataType.STRING))
            mobHash = lmEntity.pdc.get(NamespacedKeys.mobHash, PersistentDataType.STRING)

        val scheduler = SchedulerWrapper(lmEntity.livingEntity) {
            showEffectiveValues(lmEntity, showOnConsole, mobHash)
            lmEntity.free()
            if (showOnConsole) sender.sendMessage("Effective rules have been printed in the console")
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDelayed(25L)
    }

    private fun buildShowEffectiveSuggestions(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ) : CompletableFuture<Suggestions>{
        var hasConsole = false
        var hasLookingAt = false
        val words = getOptionalResults(ctx, mutableListOf("option1", "option2"))

        for (i in 3..<words.size){
            val word = words[i]
            if (word.startsWith("console", ignoreCase = true))
                hasConsole = true
            else if (word.startsWith("looking-at", ignoreCase = true))
                hasLookingAt = true
        }

        if (!hasConsole) builder.suggest("console")
        if (!hasLookingAt) builder.suggest("looking-at")

        return builder.buildFuture()
    }

    fun getMobBeingLookedAt(
        player: Player,
        findNearbyEntities: Boolean,
        sender: CommandSender
    ): LivingEntityWrapper? {
        var livingEntity: LivingEntity? = null
        var lmEntity: LivingEntityWrapper? = null
        val eye = player.eyeLocation
        val entities: SortedMap<Double, LivingEntity> = TreeMap()

        for (entity in player.getNearbyEntities(10.0, 10.0, 10.0)) {
            if (entity !is LivingEntity) continue

            if (findNearbyEntities) {
                val distance = entity.location.distanceSquared(player.location)
                entities[distance] = entity
            } else {
                val toEntity: Vector = entity.eyeLocation.toVector().subtract(eye.toVector())
                val dot = toEntity.normalize().dot(eye.direction)
                if (dot >= 0.975) {
                    livingEntity = entity
                    break
                }
            }
        }

        if (!findNearbyEntities && livingEntity == null)
            MessagesHelper.showMessage(sender, "command.levelledmobs.rules.no-entities-visible")
        else if (findNearbyEntities && entities.isEmpty())
            MessagesHelper.showMessage(sender, "command.levelledmobs.rules.no-entities-near")
        else {
            if (findNearbyEntities)
                livingEntity = entities[entities.firstKey()]

            createParticleEffect(livingEntity!!.location)
            lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        }

        return lmEntity
    }

    private fun createParticleEffect(location: Location) {
        val world = location.world ?: return

        val scheduler = SchedulerWrapper { spawnParticles(location, world) }
        scheduler.locationForRegionScheduler = location
        scheduler.runAsync()
    }

    private fun spawnParticles(location: Location, world: World) {
        val ver = LevelledMobs.instance.ver
        val useSpell = (ver.minecraftVersion >= 1.21 && ver.revision >= 9
            || ver.majorVersion >= 22)

        try {
            for (i in 1.. 40)    {
                val scheduler = SchedulerWrapper{
                    val yPos = (i * 0.05)
                    if (useSpell)
                        createParticlesWithSpell(location, yPos)
                    else
                        world.spawnParticle(Particle.EFFECT, location, 20, 0.0, 0.0, 0.0, 0.1)
                }
                scheduler.run()

                // this thread is async, no blocking occurs
                Thread.sleep(30)
            }
        }
        catch (_: InterruptedException) {}
        catch (e: IllegalArgumentException){
            Log.war("Unable to create particle effect, " + e.message)
        }
    }

    private fun createParticlesWithSpell(
        location: Location,
        yPos: Double
    ){
        val particle = Particle.EFFECT
        val spell = Particle.Spell(Color.PURPLE, 50f)
        location.world.spawnParticle(particle, location, 20, 0.0, yPos, 0.0, 0.1, spell)
    }

    private fun showEffectiveValues(
        lmEntity: LivingEntityWrapper,
        showOnConsole: Boolean,
        mobHash: String?
    ) {
        val values = mutableMapOf<RuleInfo.RuleSortingInfo, String>()
        val effectiveRules = lmEntity.getApplicableRules()
        val sb = StringBuilder()
        val effectiveInfosAlreadyProcessed = mutableMapOf<String, RuleInfo.RuleSortingInfo>()

        if (effectiveRules.isEmpty()) {
            if (showOnConsole) {
                Log.inf(
                    "$sb\n" + MessagesHelper.getMessage( "command.levelledmobs.rules.no-effective-rules").replace(
                        LevelledMobs.instance.configUtils.prefix + " ", ""
                    )
                )
            }
            else
                showMessage("command.levelledmobs.rules.no-effective-rules")

            return
        }

        try {
            for (i in effectiveRules.indices) {
                val pi = effectiveRules[i]

                for (f in pi::class.java.declaredFields) {
                    f.trySetAccessible()

                    if (f.isAnnotationPresent(DoNotMerge::class.java)) continue
                    if (f.isAnnotationPresent(DoNotShow::class.java)) continue
                    val value = f.get(pi) ?: continue
                    if (value.toString().isEmpty()) continue

                    var ruleName = f.name
                    var showValue: String? = null
                    var ruleInfoType = RuleType.MISC
                    val ruleTypeInfo = f.getAnnotation(RuleFieldInfo::class.java)
                    if (ruleTypeInfo != null) {
                        ruleInfoType = ruleTypeInfo.ruleType
                        ruleName = ruleTypeInfo.value
                    }

                    if (value is Map<*, *> && value.isEmpty()) continue
                    if (value is List<*> && value.isEmpty()) continue

                    val ruleInfo = RuleInfo.RuleSortingInfo(
                        ruleInfoType,
                        ruleName
                    )

                    if (value is PlayerLevellingStrategy)
                        showValue = getPlayerLevellingFormatting(value, lmEntity)
                    else if (value is EffectiveInfo) {
                        if (effectiveInfosAlreadyProcessed.contains(ruleName)) {
                            pi.ruleSourceNames[f.name] = pi.ruleName
                            continue
                        }

                        showValue = value.getEffectiveInfo(lmEntity)
                        effectiveInfosAlreadyProcessed[pi.ruleName] = ruleInfo
                    }

                    if (value is Enum<*> &&
                        ("NONE" == value.toString() || "NOT_SPECIFIED" == value.toString())
                    ) continue

                    if (showValue == null) showValue = "&b$value&r"

                    showValue += ", &1source: " + (if (pi.ruleSourceNames.containsKey(f.name)) pi.ruleSourceNames[f.name] else pi.ruleName)
                    showValue += "&r"

                    values[ruleInfo] = showValue
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (mobHash != null) {
            val mobHashInfo = RuleInfo.RuleSortingInfo(
                RuleType.MISC,
                "mob hash"
            )
            values[mobHashInfo] = mobHash
        }
        if (lmEntity.mobExternalTypes.isNotEmpty()) {
            val externalMobTypeMisc = RuleInfo.RuleSortingInfo(
                RuleType.MISC,
                "external plugin type"
            )
            values[externalMobTypeMisc] = lmEntity.mobExternalTypes.toString()
        }

        RuleInfo.formatRule(sb, values)

        if (showOnConsole)
            Log.inf(sb.toString())
        else
            commandSender!!.sendMessage(colorizeAll(sb.toString()))
    }

    private fun getPlayerLevellingFormatting(
        opts: PlayerLevellingStrategy,
        lmEntity: LivingEntityWrapper
    ): String {
        val sb = StringBuilder("value: ")
        var userId: String? = null
        var plValue: String? = null

        if (lmEntity.pdc.has(NamespacedKeys.playerLevellingId))
            userId = lmEntity.pdc.get(NamespacedKeys.playerLevellingId, PersistentDataType.STRING)

        if (lmEntity.pdc.has(NamespacedKeys.playerLevellingValue))
            plValue = lmEntity.pdc.get(NamespacedKeys.playerLevellingValue, PersistentDataType.STRING)

        if (plValue != null) sb.append(plValue)

        var foundName = false
        if (userId != null) {
            val uuid = UUID.fromString(userId)
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                foundName = true
                if (plValue != null) sb.append(", ")

                sb.append("plr: ").append(player.name)
            }
        }

        if (plValue != null || foundName)
            sb.append(", ")

        sb.append(opts)
        return sb.toString()
    }
}