package io.github.arcaneplugins.levelledmobs.commands.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
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
import io.github.arcaneplugins.levelledmobs.commands.MessagesHelper.showMessage
import io.github.arcaneplugins.levelledmobs.enums.RuleType
import io.github.arcaneplugins.levelledmobs.listeners.EntitySpawnListener
import io.github.arcaneplugins.levelledmobs.rules.strategies.PlayerLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import org.bukkit.Bukkit
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
object RulesSubcommand {
    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("rules")
            .withPermission("levelledmobs.command.rules")
            .withShortDescription("Used to view various rules.")
            .withFullDescription("Used to view various rules.")
            .executes(CommandExecutor { sender, _ ->
                showMessage(sender, "command.levelledmobs.rules.incomplete-command") }
            )
            .withSubcommands(
                CommandAPICommand("show-all")
                    .withOptionalArguments(StringArgument("console")
                        .includeSuggestions(ArgumentSuggestions.strings("console")))
                    .executes(CommandExecutor { sender, args -> showAllRules(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("show-rule")
                    .withArguments(StringArgument("rulename")
                        .replaceSuggestions(ArgumentSuggestions.strings { _ -> getAllRuleNames() }))
                    .withOptionalArguments(StringArgument("console")
                        .includeSuggestions(ArgumentSuggestions.strings("console")))
                    .executes(CommandExecutor { sender, args -> showRule(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("show-effective")
                    .withOptionalArguments(
                        ListArgumentBuilder<String>("values")
                        .withList(mutableListOf("console", "looking-at"))
                        .withStringMapper()
                        .buildGreedy()
                    )
                    .executes(CommandExecutor { sender, args -> showEffectiveRules(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("help-discord")
                    .executes(CommandExecutor { sender, _ ->
                        val message = MessagesHelper.getMessage("command.levelledmobs.rules.discord-invite")
                        showHyperlink(sender, message, "https://discord.gg/arcaneplugins-752310043214479462")
                    })
            )
            .withSubcommands(
                CommandAPICommand("help-wiki")
                    .executes(CommandExecutor { sender, _ ->
                        val message = MessagesHelper.getMessage("command.levelledmobs.rules.wiki-link")
                        showHyperlink(sender, message, "https://arcaneplugins.gitbook.io/levelledmobs-the-ultimate-mob-levelling-solution/")
                    })
            )
            .withSubcommands(
                CommandAPICommand("reset")
                    .withOptionalArguments(StringArgument("difficulty")
                        .includeSuggestions(ArgumentSuggestions.strings(
                            "vanilla", "bronze", "silver", "gold", "platinum")))
                    .withOptionalArguments(StringArgument("confirm"))
                    .executes(CommandExecutor { sender, args -> resetRules(sender, args) })
            )
            .withSubcommands(
                CommandAPICommand("force-all")
                    .executes(CommandExecutor { sender, _ -> forceRelevel(sender) })
            )
            .withSubcommands(
                CommandAPICommand("show-temp-disabled")
                    .executes(CommandExecutor { sender, _ -> showTempDisabled(sender) })
            )
    }

    private fun showAllRules(
        sender: CommandSender,
        args: CommandArguments
    ){
        if (sender is Player) {
            showMessage(sender,"command.levelledmobs.rules.console-rules")
        }

        val showOnConsole = (args.get("console") as? String != null)
        val main = LevelledMobs.instance
        val sb = StringBuilder()

        for (rpi in main.rulesParsingManager.rulePresets.values) {
            sb.append(
                "\n&r--------------------------------- Preset rule ----------------------------------"
            )
            sb.append(rpi.formatRulesVisually(false, mutableListOf("ruleIsEnabled")))
        }

        sb.append(
            "\n&r--------------------------------- Default values -------------------------------"
        )
        sb.append(main.rulesParsingManager.defaultRule!!.formatRulesVisually())

        for (rpi in main.rulesParsingManager.customRules) {
            sb.append(
                "\n&r--------------------------------- Custom rule ----------------------------------"
            )
            sb.append(rpi.formatRulesVisually())
        }
        sb.append(
            "\n&r--------------------------------------------------------------------------------------"
        )

        if (showOnConsole) {
            Log.inf(colorizeAll(sb.toString()))
        } else {
            sender.sendMessage(colorizeAll(sb.toString()))
        }
    }

    private fun getAllRuleNames(): Array<String>{
        val allRuleNames = mutableListOf<String>()
        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            allRuleNames.add(ruleInfo.ruleName.replace(" ", "_"))
        }

        return allRuleNames.toTypedArray()
    }

    private fun showTempDisabled(sender: CommandSender) {
        LevelledMobs.instance.rulesManager.showTempDisabledRules(sender)
    }

    private fun forceRelevel(sender: CommandSender) {
        //TODO: make this work in Folia
        if (LevelledMobs.instance.ver.isRunningFolia) {
            sender.sendMessage("Sorry this command doesn't work in Folia")
            return
        }

        var worldCount = 0
        var entityCount = 0
        val main = LevelledMobs.instance

        main.reloadLM(sender)
        val isUsingPlayerLevelling = main.rulesManager.isPlayerLevellingEnabled()

        for (world in Bukkit.getWorlds()) {
            worldCount++
            for (entity in world.entities) {
                if (entity !is LivingEntity || entity is Player) {
                    continue
                }

                var doContinue = false
                synchronized(entity.getPersistentDataContainer()) {
                    if (entity.getPersistentDataContainer().has(
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

        showMessage(
            sender,
            "command.levelledmobs.rules.rules-reprocessed",
            arrayOf("%entitycount%", "%worldcount%"),
            arrayOf(entityCount.toString(), worldCount.toString())
        )
    }

    private fun resetRules(
        sender: CommandSender,
        args: CommandArguments
    ) {
        val difficultyStr = args.get("difficulty") as? String
        val confirm = args.get("confirm") as? String
        if (difficultyStr == null) {
            showMessage(sender, "command.levelledmobs.rules.reset")
            return
        }

        val difficulty: ResetDifficulty = when (difficultyStr.lowercase(Locale.getDefault())) {
            "vanilla" -> ResetDifficulty.VANILLA
            "bronze" -> ResetDifficulty.BRONZE
            "silver" -> ResetDifficulty.SILVER
            "gold" -> ResetDifficulty.GOLD
            "platinum" -> ResetDifficulty.PLATINUM
            else -> ResetDifficulty.UNSPECIFIED
        }

        if (difficulty == ResetDifficulty.UNSPECIFIED) {
            showMessage(sender, "command.levelledmobs.rules.invalid-difficulty", "%difficulty%", difficultyStr)
            return
        }

        if (confirm == null || !"confirm".equals(confirm, ignoreCase = true)) {
            showMessage(sender, "command.levelledmobs.rules.reset-syntax", "%difficulty%", difficultyStr)
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
            showMessage(sender,
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
                    if (!rulesBackupFile.exists()) {
                        break
                    }
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
            showMessage(sender, "command.levelledmobs.rules.reset-complete")
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

        if (LevelledMobs.instance.ver.isRunningPaper) {
            PaperUtils.sendHyperlink(sender, message, url)
        } else {
            SpigotUtils.sendHyperlink(sender, message, url)
        }
    }

    private fun showRule(
        sender: CommandSender,
        args: CommandArguments
    ) {
        val ruleName = args.get("rulename") as? String
        var showOnConsole = sender is ConsoleCommandSender
        if (args.get("console") as? String != null)
            showOnConsole = true

        if (ruleName == null) {
            showMessage(sender, "command.levelledmobs.rules.rule-name-missing")
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
            showMessage(sender, "command.levelledmobs.rules.rule-name-invalid")
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
        if (showOnConsole) {
            Log.inf(sb.toString())
        } else {
            sender.sendMessage(colorizeAll(sb.toString()))
        }
    }

    private fun showEffectiveRules(
        sender: CommandSender,
        args: CommandArguments
    ) {
        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("Must be run by a player")
            return
        }

        var showOnConsole = false
        var findNearbyEntities = true
        val input = args.get("values")
        if (input != null){
            @Suppress("UNCHECKED_CAST") val inputValue = (input as java.util.ArrayList<String>).toMutableSet()
            for (key in inputValue){
                if ("console".equals(key, ignoreCase = true))
                    showOnConsole = true
                else if ("looking-at".equals(key, ignoreCase = true))
                    findNearbyEntities = false
            }
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
            arrayOf("%mobname%", "%entitytype%", "%location%", "%world%", "%level%"),
            arrayOf(
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

        if (!showOnConsole) {
            sb.setLength(0)
        }

        var mobHash: String? = null
        if (lmEntity.pdc.has(NamespacedKeys.mobHash, PersistentDataType.STRING)) {
            mobHash = lmEntity.pdc.get(NamespacedKeys.mobHash, PersistentDataType.STRING)
        }

        val scheduler = SchedulerWrapper(lmEntity.livingEntity) {
            showEffectiveValues(sender, lmEntity, showOnConsole, mobHash)
            lmEntity.free()
            if (showOnConsole) sender.sendMessage("Effective rules have been printed in the console")
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDelayed(25L)
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
            if (entity !is LivingEntity) {
                continue
            }

            if (findNearbyEntities) {
                val distance = entity.getLocation().distanceSquared(player.location)
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

        if (!findNearbyEntities && livingEntity == null) {
            showMessage(sender, "command.levelledmobs.rules.no-entities-visible")
        } else if (findNearbyEntities && entities.isEmpty()) {
            showMessage(sender, "command.levelledmobs.rules.no-entities-near")
        } else {
            if (findNearbyEntities) {
                livingEntity = entities[entities.firstKey()]
            }

            createParticleEffect(livingEntity!!.location)
            lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        }

        return lmEntity
    }

    private fun createParticleEffect(location: Location) {
        val world = location.world ?: return

        val scheduler = SchedulerWrapper { spawnParticles(location, world) }
        scheduler.locationForRegionScheduler = location
        scheduler.run()
    }

    private fun spawnParticles(location: Location, world: World) {
        try {
            for (i in 0..9) {
                world.spawnParticle(Particle.EFFECT, location, 20, 0.0, 0.0, 0.0, 0.1)
                Thread.sleep(50)
            }
        } catch (ignored: InterruptedException) {}
    }

    private fun showEffectiveValues(
        sender: CommandSender,
        lmEntity: LivingEntityWrapper,
        showOnConsole: Boolean,
        mobHash: String?
    ) {
        val values = mutableMapOf<RuleInfo.RuleSortingInfo, String>()
        val effectiveRules = lmEntity.getApplicableRules()
        val sb = StringBuilder()

        if (effectiveRules.isEmpty()) {
            if (showOnConsole) {
                Log.inf(
                    "$sb\n" + MessagesHelper.getMessage( "command.levelledmobs.rules.no-effective-rules").replace(
                        LevelledMobs.instance.configUtils.prefix + " ", ""
                    )
                )
            } else {
                showMessage(sender, "command.levelledmobs.rules.no-effective-rules")
            }
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

                    if (value is PlayerLevellingStrategy) {
                        showValue = getPlayerLevellingFormatting(value, lmEntity)
                    }

                    if (value is Enum<*> &&
                        ("NONE" == value.toString() || "NOT_SPECIFIED" == value.toString())
                    ) continue

                    if (showValue == null) {
                        showValue = "&b$value&r"
                    }
                    showValue += ", &1source: " + (if (pi.ruleSourceNames.containsKey(f.name)) pi.ruleSourceNames[f.name] else pi.ruleName)
                    showValue += "&r"

                    val ruleInfo = RuleInfo.RuleSortingInfo(
                        ruleInfoType,
                        ruleName
                    )
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

        var hadConditions = false
        var hadApplySettings = false
        var hadStrategies = false
        var hadMisc = false

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.CONDITION }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadConditions){
                hadConditions = true
                sb.append("\n&lConditions:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.APPLY_SETTING }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadApplySettings){
                hadApplySettings = true
                sb.append("\n&lApply Settings&r:")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.STRATEGY }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadStrategies){
                hadStrategies = true
                sb.append("\n&lStrategies:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType != RuleType.CONDITION &&
                    v.key.ruleType != RuleType.APPLY_SETTING &&
                    v.key.ruleType != RuleType.STRATEGY &&
                    v.key.ruleType != RuleType.NO_CATEGORY }
            .iterator()
        ){
            if (!hadMisc){
                hadMisc = true
                sb.append("\n&lMisc:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        if (showOnConsole) {
            Log.inf(sb.toString())
        } else {
            sender.sendMessage(colorizeAll(sb.toString()))
        }
    }

    private fun getPlayerLevellingFormatting(
        opts: PlayerLevellingStrategy,
        lmEntity: LivingEntityWrapper
    ): String {
        val sb = StringBuilder("value: ")

        var userId: String? = null
        var plValue: String? = null

        if (lmEntity.pdc.has(NamespacedKeys.playerLevellingId)) {
            userId = lmEntity.pdc.get(NamespacedKeys.playerLevellingId, PersistentDataType.STRING)
        }
        if (lmEntity.pdc.has(NamespacedKeys.playerLevellingValue)) {
            plValue = lmEntity.pdc.get(NamespacedKeys.playerLevellingValue, PersistentDataType.STRING)
        }

        if (plValue != null) {
            sb.append(plValue)
        }

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

        if (plValue != null || foundName) {
            sb.append(", ")
        }

        sb.append(opts)
        return sb.toString()
    }
}