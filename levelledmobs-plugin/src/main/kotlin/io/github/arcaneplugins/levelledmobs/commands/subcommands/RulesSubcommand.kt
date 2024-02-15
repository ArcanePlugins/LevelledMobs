package io.github.arcaneplugins.levelledmobs.commands.subcommands

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.LinkedList
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap
import java.util.TreeSet
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import io.github.arcaneplugins.levelledmobs.annotations.DoNotShow
import io.github.arcaneplugins.levelledmobs.annotations.RuleFieldInfo
import io.github.arcaneplugins.levelledmobs.enums.RuleType
import io.github.arcaneplugins.levelledmobs.rules.PlayerLevellingOptions
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
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
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * Shows the current rules as parsed from the various config files
 *
 * @author stumper66
 * @since 3.0.0
 */
class RulesSubcommand : MessagesBase(), Subcommand {
    override fun parseSubcommand(
        sender: CommandSender,
        label: String,
        args: Array<String>
    ) {
        commandSender = sender
        messageLabel = label
        val main = LevelledMobs.instance

        if (!sender.hasPermission("levelledmobs.command.rules")) {
            main.configUtils.sendNoPermissionMsg(sender)
            return
        }

        if (args.size == 1) {
            showMessage("command.levelledmobs.rules.incomplete-command")
            return
        }

        var showOnConsole = false
        var findNearbyEntities = false

        for (i in 2 until args.size) {
            if ("/console".equals(args[i], ignoreCase = true)) {
                showOnConsole = true
            } else if ("/near".equals(args[i], ignoreCase = true)) {
                findNearbyEntities = true
            }
        }

        if ("show-all".equals(args[1], ignoreCase = true)) {
            if (sender is Player) {
                showMessage("command.levelledmobs.rules.console-rules")
            }

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
        } else if ("show-effective".equals(args[1], ignoreCase = true)) {
            if (sender !is Player) {
                showMessage("common.players-only")
                return
            }

            showEffectiveRules(sender, showOnConsole, findNearbyEntities)
        } else if ("show-rule".equals(args[1], ignoreCase = true)) {
            showRule(sender, args)
        } else if ("help-discord".equals(args[1], ignoreCase = true)) {
            val message = getMessage("command.levelledmobs.rules.discord-invite")
            showHyperlink(sender, message, "https://www.discord.io/arcaneplugins")
        } else if ("help-wiki".equals(args[1], ignoreCase = true)) {
            val message = getMessage("command.levelledmobs.rules.wiki-link")
            showHyperlink(sender, message, "https://github.com/ArcanePlugins/LevelledMobs/wiki")
        } else if ("reset".equals(args[1], ignoreCase = true)) {
            resetRules(sender, args)
        } else if ("force-all".equals(args[1], ignoreCase = true)) {
            forceRelevel(sender)
        } else if ("show-temp-disabled".equals(args[1], ignoreCase = true)) {
            showTempDisabled(sender)
        } else {
            showMessage("common.invalid-command")
        }
    }

    private fun showTempDisabled(sender: CommandSender) {
        val isConsoleSender = sender is ConsoleCommandSender
        sender.sendMessage(LevelledMobs.instance.rulesManager.showTempDisabledRules(isConsoleSender))
    }

    private fun forceRelevel(sender: CommandSender) {
        var worldCount = 0
        var entityCount = 0
        val main = LevelledMobs.instance

        main.reloadLM(sender)

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
                main.mobsQueueManager.addToQueue(QueueItem(lmEntity, null))
                lmEntity.free()
            }
        }

        showMessage(
            "command.levelledmobs.rules.rules-reprocessed",
            arrayOf("%entitycount%", "%worldcount%"),
            arrayOf(entityCount.toString(), worldCount.toString())
        )
    }

    private fun resetRules(
        sender: CommandSender,
        args: Array<String>
    ) {
        if (args.size < 3 || args.size > 4) {
            showMessage("command.levelledmobs.rules.reset")
            return
        }

        val difficulty: ResetDifficulty = when (args[2].lowercase(Locale.getDefault())) {
            "vanilla" -> ResetDifficulty.VANILLA
            "basic" -> ResetDifficulty.BASIC
            "average" -> ResetDifficulty.AVERAGE
            "advanced" -> ResetDifficulty.ADVANCED
            "extreme" -> ResetDifficulty.EXTREME
            else -> ResetDifficulty.UNSPECIFIED
        }

        if (difficulty == ResetDifficulty.UNSPECIFIED) {
            showMessage("command.levelledmobs.rules.invalid-difficulty", "%difficulty%", args[2])
            return
        }

        if (args.size == 3) {
            showMessage("command.levelledmobs.rules.reset-syntax", "%difficulty%", args[2])
            return
        }

        resetRules(sender, difficulty)
    }

    private fun resetRules(
        sender: CommandSender,
        difficulty: ResetDifficulty
    ) {
        val main = LevelledMobs.instance
        val prefix = main.configUtils.prefix
        showMessage(
            "command.levelledmobs.rules.resetting", "%difficulty%",
            difficulty.toString()
        )

        val filename = "rules.yml"
        val replaceWhat = arrayOf("    - average_challenge", "")
        val replaceWith = arrayOf("    #- average_challenge", "")

        when (difficulty) {
            ResetDifficulty.VANILLA -> {
                replaceWhat[1] = "#- vanilla_challenge"
                replaceWith[1] = "- vanilla_challenge"
            }

            ResetDifficulty.BASIC -> {
                replaceWhat[1] = "#- basic_challenge"
                replaceWith[1] = "- basic_challenge"
            }

            ResetDifficulty.ADVANCED -> {
                replaceWhat[1] = "#- advanced_challenge"
                replaceWith[1] = "- advanced_challenge"
            }

            ResetDifficulty.EXTREME -> {
                replaceWhat[1] = "#- extreme_challenge"
                replaceWith[1] = "- extreme_challenge"
            }

            ResetDifficulty.AVERAGE, ResetDifficulty.UNSPECIFIED -> {}
        }
        try {
            main.getResource(filename).use { stream ->
                if (stream == null) {
                    Log.sev("$prefix Input stream was null")
                    return
                }
                var rulesText =
                    String(stream.readAllBytes(), StandardCharsets.UTF_8)
                if (difficulty != ResetDifficulty.AVERAGE) {
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

        showMessage("command.levelledmobs.rules.reset-complete")
        main.reloadLM(sender)
    }

    private enum class ResetDifficulty {
        VANILLA, BASIC, AVERAGE, ADVANCED, EXTREME, UNSPECIFIED
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
        args: Array<String>
    ) {
        if (args.size < 3) {
            showMessage("command.levelledmobs.rules.rule-name-missing")
            return
        }

        var showOnConsole = sender is ConsoleCommandSender
        var foundRule: String? = null
        val allRuleNames: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            allRuleNames[ruleInfo.ruleName.replace(" ", "_")] = ruleInfo
        }

        var badRuleName: String? = null

        for (i in 2 until args.size) {
            val arg = args[i].lowercase(Locale.getDefault())

            if (foundRule == null && arg.isNotEmpty() && !arg.startsWith("/")) {
                if (allRuleNames.containsKey(arg)) {
                    foundRule = args[i]
                } else if (badRuleName == null) {
                    badRuleName = args[i]
                }
            }

            if ("/console".equals(arg, ignoreCase = true)) {
                showOnConsole = true
            }
        }

        if (badRuleName != null) {
            showMessage("command.levelledmobs.rules.rule-name-invalid", "%rulename%", badRuleName)
            return
        }
        if (foundRule == null) {
            showMessage("command.levelledmobs.rules.rule-name-missing")
            return
        }

        val rule = allRuleNames[foundRule]

        val sb = StringBuilder()
        sb.append(
            getMessage(
                "command.levelledmobs.rules.showing-rules", "%rulename%",
                rule!!.ruleName
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
        player: Player,
        showOnConsole: Boolean,
        findNearbyEntities: Boolean
    ) {
        val lmEntity: LivingEntityWrapper = getMobBeingLookedAt(player, findNearbyEntities, this.commandSender!!)
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
        val messages = getMessage(
            "command.levelledmobs.rules.effective-rules",
            arrayOf("%mobname%", "%entitytype%", "%location%", "%world%", "%level%"),
            arrayOf(
                entityName, lmEntity.nameIfBaby, locationStr, lmEntity.worldName,
                mobLevel
            )
        )

        val sb = StringBuilder()
        sb.append(messages.joinToString("\n").replace(
            LevelledMobs.instance.configUtils.prefix + " ", ""))

        player.sendMessage(sb.toString())
        if (!showOnConsole) {
            sb.setLength(0)
        }

        var mobHash: String? = null
        if (lmEntity.pdc.has(NamespacedKeys.mobHash, PersistentDataType.STRING)) {
            mobHash = lmEntity.pdc.get(NamespacedKeys.mobHash, PersistentDataType.STRING)
        }

        val scheduler = SchedulerWrapper(lmEntity.livingEntity) {
            showEffectiveValues(player, lmEntity, showOnConsole, mobHash)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDelayed(25L)
    }

    fun getMobBeingLookedAt(
        player: Player,
        findNearbyEntities: Boolean,
        sender: CommandSender
    ): LivingEntityWrapper? {
        this.commandSender = sender
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
            showMessage("command.levelledmobs.rules.no-entities-visible")
        } else if (findNearbyEntities && entities.isEmpty()) {
            showMessage("command.levelledmobs.rules.no-entities-near")
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
                world.spawnParticle(Particle.SPELL, location, 20, 0.0, 0.0, 0.0, 0.1)
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
        val printedKeys = mutableListOf<String>()
        val effectiveRules = lmEntity.getApplicableRules()
        val sb = StringBuilder()

        if (effectiveRules.isEmpty()) {
            if (showOnConsole) {
                Log.inf(
                    sb.toString() + "\n" + getMessage("command.levelledmobs.rules.no-effective-rules").replace(
                        LevelledMobs.instance.configUtils.prefix + " ", ""
                    )
                )
            } else {
                showMessage("command.levelledmobs.rules.no-effective-rules")
            }
            return
        }

        try {
            for (i in effectiveRules.indices) {
                val pi = effectiveRules[i]

                for (f in pi::class.declaredMemberProperties) {
                    if (f.visibility == KVisibility.PRIVATE) continue
                    if (f.hasAnnotation<DoNotMerge>()) continue
                    if (f.hasAnnotation<DoNotShow>()) continue
                    val value = f.getter.call(pi) ?: continue
                    if (value.toString().isEmpty()) continue

                    var ruleName = f.name
                    var showValue: String? = null
                    var ruleInfoType = RuleType.MISC
                    val ruleTypeInfo = f.findAnnotation<RuleFieldInfo>()
                    if (ruleTypeInfo != null) {
                        ruleInfoType = ruleTypeInfo.ruleType
                        ruleName = ruleTypeInfo.value
                    }

                    if (printedKeys.contains(f.name)) continue
                    if (value is Map<*, *> && value.isEmpty()) continue
                    if (value is List<*> && value.isEmpty()) continue

                    if (value is PlayerLevellingOptions) {
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
                    printedKeys.add(f.name)
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

    override fun parseTabCompletions(
        sender: CommandSender,
        args: Array<String>
    ): MutableList<String> {
        if (!sender.hasPermission("levelledmobs.command.rules")) {
            return mutableListOf()
        }

        val suggestions: MutableList<String> = LinkedList()

        if (args.size == 2) {
            return mutableListOf(
                "force-all", "help-discord", "help-wiki", "reset", "show-all",
                "show-effective", "show-rule", "show-temp-disabled"
            )
        } else if (args.size >= 3) {
            if ("reset".equals(args[1], ignoreCase = true) && args.size == 3) {
                suggestions.addAll(listOf("vanilla", "basic", "average", "advanced", "extreme"))
            } else if ("show-all".equals(args[1], ignoreCase = true)) {
                var showOnConsole = false
                for (i in 2 until args.size) {
                    val arg = args[i].lowercase(Locale.getDefault())

                    if ("/console".equals(arg, ignoreCase = true)) {
                        showOnConsole = true
                        break
                    }
                }
                if (!showOnConsole) {
                    suggestions.add("/console")
                }
            } else if ("show-rule".equals(args[1], ignoreCase = true) || "show-effective".equals(
                    args[1], ignoreCase = true
                )
            ) {
                val isShowRule = "show-rule".equals(args[1], ignoreCase = true)
                val isEffective = "show-effective".equals(args[1], ignoreCase = true)
                var showOnConsole = false
                var findNearbyEntities = false
                var foundValue = false
                val allRuleNames: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
                for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
                    allRuleNames.add(ruleInfo.ruleName.replace(" ", "_"))
                }

                for (i in 2 until args.size) {
                    val arg = args[i].lowercase(Locale.getDefault())

                    if (arg.isNotEmpty() && !arg.startsWith("/") && allRuleNames.contains(arg)) {
                        foundValue = true
                    }

                    if ("/console".equals(arg, ignoreCase = true)) {
                        showOnConsole = true
                    } else if ("/near".equals(arg, ignoreCase = true)) {
                        findNearbyEntities = true
                    }
                }
                if (!showOnConsole) {
                    suggestions.add("/console")
                }
                if (isEffective && !findNearbyEntities) {
                    suggestions.add("/near")
                }
                if (isShowRule && !foundValue) {
                    suggestions.addAll(allRuleNames)
                }
            }
        }

        if (suggestions.isEmpty()) {
            return mutableListOf()
        }
        return suggestions
    }

    private fun getPlayerLevellingFormatting(
        opts: PlayerLevellingOptions,
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