package io.github.arcaneplugins.levelledmobs.debug

import java.time.Duration
import java.time.Instant
import java.util.LinkedList
import java.util.Locale
import java.util.function.Supplier
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerResult
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import kotlin.math.floor

/**
 * Provides the logic for the debug system
 *
 * @author stumper66
 * @since 3.14.0
 */
class DebugManager {
    private val defaultPlayerDistance = 16
    var isEnabled = false
        private set
    var isTimerEnabled = false
        private set
    var bypassAllFilters = false
        private set
    private var timerEndTime: Instant? = null
    private var timerTask: SchedulerResult? = null
    val filterDebugTypes = mutableSetOf<DebugType>()
    val filterEntityTypes = mutableSetOf<EntityType>()
    val filterRuleNames = mutableSetOf<String>()
    val filterPlayerNames = mutableSetOf<String>()
    var excludedEntityTypes = mutableListOf<String>()
    var playerThatEnabledDebug: Player? = null
    var listenFor: ListenFor = ListenFor.BOTH
    var outputType: OutputTypes = OutputTypes.TO_CONSOLE
    var maxPlayerDistance: Int? = null
    var minYLevel: Int? = null
    var maxYLevel: Int? = null
    var disableAfter: Long? = null
    var disableAfterStr: String? = null

    init {
        instance = this
        buildExcludedEntityTypes()
    }

    fun enableDebug(sender: CommandSender, usetimer: Boolean, bypassFilters: Boolean) {
        if (sender is Player) this.playerThatEnabledDebug = sender
        this.bypassAllFilters = bypassFilters
        this.isEnabled = true
        checkTimerSettings(usetimer)
    }

    fun disableDebug() {
        this.isEnabled = false
        this.isTimerEnabled = false
        disableTimer()
    }

    private fun disableTimer() {
        isTimerEnabled = false

        if (this.timerTask == null) {
            return
        }

        timerTask!!.cancelTask()
        this.timerTask = null
    }

    private fun checkTimerSettings(useTimer: Boolean) {
        if (!isEnabled) return

        val canUseTimer = this.disableAfter != null && disableAfter!! > 0L
        if (!useTimer || !canUseTimer) {
            disableTimer()
            return
        }

        this.timerEndTime = Instant.now().plusMillis(disableAfter!!)

        if (!this.isTimerEnabled) {
            this.isTimerEnabled = true
            val wrapper = SchedulerWrapper { this.timerLoop() }
            this.timerTask = wrapper.runTaskTimerAsynchronously(20, 20)
        }
    }

    companion object{
        private lateinit var instance: DebugManager

        fun log(
            debugType: DebugType,
            ruleInfo: RuleInfo,
            lmEntity: LivingEntityWrapper,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, ruleInfo, lmEntity, null, null, msg.get()!!)
        }

        fun log(
            debugType: DebugType,
            ruleInfo: RuleInfo,
            lmInterface: LivingEntityInterface,
            ruleResult: Boolean,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, ruleInfo, lmInterface, null, ruleResult, msg.get()!!)
        }

        fun log(
            debugType: DebugType,
            lmEntity: LivingEntityWrapper,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, null, lmEntity, null, null, msg.get()!!)
        }

        fun log(
            debugType: DebugType,
            lmEntity: LivingEntityWrapper,
            result: Boolean,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, null, lmEntity, null, result, msg.get()!!)
        }

        fun log(
            debugType: DebugType,
            entity: Entity,
            result: Boolean,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, null, null, entity, result, msg.get()!!)
        }

        fun log(
            debugType: DebugType,
            entity: Entity,
            msg: Supplier<String?>
        ) {
            instance.logInstance(debugType, null, null, entity, null, msg.get()!!)
        }

        /**
         * Sends a debug message to console if enabled in settings
         *
         * @param debugType Reference to whereabouts the debug log is called so that it can be traced
         * back easily
         * @param msg       Message to help de-bugging
         */
        fun log(debugType: DebugType, msg: Supplier<String?>) {
            instance.logInstance(debugType, null, null, null, null, msg.get()!!)
        }
    }

    private fun logInstance(
        debugType: DebugType,
        ruleInfo: RuleInfo?,
        lmInterface: LivingEntityInterface?,
        entity: Entity?,
        ruleResult: Boolean?,
        origMsg: String
    ) {
        if (!isEnabled) return
        var msg = origMsg

        // now you have to pass all of the filters if they are configured
        if (!bypassAllFilters) {
            if (filterDebugTypes.isNotEmpty() && !filterDebugTypes.contains(debugType)) return

            if (ruleInfo != null && filterRuleNames.isNotEmpty() &&
                !filterRuleNames.contains(ruleInfo.ruleName.replace(" ", "_")) ||
                ruleInfo == null && filterRuleNames.isNotEmpty()
            ) {
                return
            }

            if (filterEntityTypes.isNotEmpty()) {
                var et: EntityType? = null
                if (entity != null) et = entity.type
                else if (lmInterface != null) et = lmInterface.entityType
                if (!filterEntityTypes.contains(et)) return
            }

            var useEntity = entity
            if (lmInterface is LivingEntityWrapper) useEntity = lmInterface.livingEntity

            if (maxPlayerDistance != null && maxPlayerDistance!! > 0 && useEntity != null) {
                val players = getPlayers()
                var foundMatch = false
                if (players != null) {
                    for (player in players) {
                        if (player.world !== useEntity.world) continue
                        val dist = player.location.distance(useEntity.location)
                        if (dist <= maxPlayerDistance!!) {
                            foundMatch = true
                            break
                        }
                    }
                }

                if (!foundMatch) return
            }

            if (ruleResult != null && listenFor != ListenFor.BOTH) {
                if (ruleResult && listenFor == ListenFor.FAILURE) return
                if (!ruleResult && listenFor == ListenFor.SUCCESS) return
            }

            if (useEntity != null) {
                if (minYLevel != null && useEntity.location.blockY < minYLevel!!) return
                if (maxYLevel != null && useEntity.location.blockY > maxYLevel!!) return
            }
        } // end bypass all


        if (ruleResult != null) {
            msg += ", result: $ruleResult"
        }

        if (outputType == OutputTypes.TO_BOTH || outputType == OutputTypes.TO_CONSOLE) {
            Utils.logger.info("&8[&bDebug: $debugType&8]&7 $msg")
        }
        if (outputType == OutputTypes.TO_BOTH || outputType == OutputTypes.TO_CHAT) {
            if (playerThatEnabledDebug == null) {
                Utils.logger.info("No player to send chat messages to")
            } else {
                playerThatEnabledDebug!!.sendMessage(
                    colorizeAll(
                        "&8[&bDebug: $debugType&8]&7 $msg"
                    )
                )
            }
        }
    }

    private fun getPlayers(): List<Player>? {
        if (filterPlayerNames.isEmpty()) {
            return LinkedList(Bukkit.getOnlinePlayers())
        }

        val players: MutableList<Player> = LinkedList()
        for (playerName in filterPlayerNames) {
            val player = Bukkit.getPlayer(playerName)
            if (player != null) players.add(player)
        }

        return if (players.isEmpty()) null else players
    }

    fun getDebugStatus(): String {
        val sb = StringBuilder("\nDebug Status: ")
        if (isEnabled) {
            sb.append("ENABLED")
            if (isTimerEnabled) {
                sb.append("-(Time Left: ")
                sb.append(getTimeRemaining()).append(")")
            }
        } else sb.append("DISABLED")

        if (!bypassAllFilters && !hasFiltering()) return sb.toString()
        sb.append("\n--------------------------\n")
            .append("Current Filter Options:")

        if (bypassAllFilters) {
            sb.append("\n- All filters bypassed")
            return sb.toString()
        }

        if (filterDebugTypes.isNotEmpty()) {
            sb.append("\n- Debug types: ")
            sb.append(filterDebugTypes)
        }

        if (filterEntityTypes.isNotEmpty()) {
            sb.append("\n- Entity types: ")
            sb.append(filterEntityTypes)
        }

        if (filterRuleNames.isNotEmpty()) {
            sb.append("\n- Rule names: ")
            sb.append(filterRuleNames)
        }

        if (filterPlayerNames.isNotEmpty()) {
            sb.append("\n- Player names: ")
            sb.append(filterPlayerNames)
        }

        if (listenFor != ListenFor.BOTH) {
            sb.append("\n- Listen for: ")
            sb.append(listenFor.name.lowercase(Locale.getDefault()))
        }

        if (maxPlayerDistance != null) {
            sb.append("\n- Max player distance: ")
            sb.append(maxPlayerDistance)
        }

        if (minYLevel != null) {
            sb.append("\n- Min y level: ")
            sb.append(minYLevel)
        }

        if (maxYLevel != null) {
            if (minYLevel != null) sb.append(", Max y level: ")
            else sb.append("\n- Max y level: ")
            sb.append(maxYLevel)
        }

        if (outputType != OutputTypes.TO_CONSOLE) {
            sb.append("\n- Output to: ")
            sb.append(outputType.name.lowercase(Locale.getDefault()))
        }

        return sb.toString()
    }

    private fun hasFiltering(): Boolean {
        return (filterDebugTypes.isNotEmpty() ||
                filterEntityTypes.isNotEmpty() ||
                filterRuleNames.isNotEmpty() ||
                filterPlayerNames.isNotEmpty() || listenFor != ListenFor.BOTH || outputType != OutputTypes.TO_CONSOLE ||
                    maxPlayerDistance == null || maxPlayerDistance != 0 || minYLevel != null || maxYLevel != null
                )
    }

    fun resetFilters() {
        filterDebugTypes.clear()
        filterEntityTypes.clear()
        filterRuleNames.clear()
        filterPlayerNames.clear()
        listenFor = ListenFor.BOTH
        outputType = OutputTypes.TO_CONSOLE
        maxPlayerDistance = defaultPlayerDistance
        minYLevel = null
        maxYLevel = null
        disableAfter = null
        disableAfterStr = null
    }

    enum class ListenFor {
        FAILURE, SUCCESS, BOTH
    }

    enum class OutputTypes {
        TO_CONSOLE, TO_CHAT, TO_BOTH
    }

    fun isDebugTypeEnabled(debugType: DebugType): Boolean {
        if (!this.isEnabled) return false

        return filterDebugTypes.isEmpty() || filterDebugTypes.contains(debugType)
    }

    private fun timerLoop() {
        if (Instant.now().isAfter(this.timerEndTime)) {
            disableDebug()

            val msg = "Debug timer has elapsed, debugging is now disabled"
            if (outputType == OutputTypes.TO_CONSOLE || outputType == OutputTypes.TO_BOTH) {
                Utils.logger.info(msg)
            }
            if ((outputType == OutputTypes.TO_CHAT || outputType == OutputTypes.TO_BOTH)
                && playerThatEnabledDebug != null
            ) {
                playerThatEnabledDebug!!.sendMessage(msg)
            }
        }
    }

    fun timerWasChanged(
        useTimer: Boolean
    ) {
        checkTimerSettings(isTimerEnabled || useTimer)
    }

    private fun getTimeRemaining(): String? {
        if (!isEnabled || disableAfter == null || disableAfter!! <= 0 || timerEndTime == null) return null

        val duration = Duration.between(Instant.now(), timerEndTime)
        val secondsLeft = duration.seconds.toInt()
        if (secondsLeft < 60) {
            return if (secondsLeft == 1) "1 second" else "$secondsLeft seconds"
        } else if (secondsLeft < 3600) {
            val minutes = floor(secondsLeft.toDouble() / 60.0).toInt()
            val newSeconds = secondsLeft % 60
            val sb = java.lang.StringBuilder()
            sb.append(minutes)
                .append(if (minutes == 1) " minute, " else " minutes, ")
                .append(newSeconds)
                .append(if (newSeconds == 1) " second" else " seconds")
            return sb.toString()
        }

        return secondsLeft.toString()
    }

    private fun buildExcludedEntityTypes() {
        this.excludedEntityTypes = mutableListOf(
            "AREA_EFFECT_CLOUD",
            "ARMOR_STAND",
            "ARROW",
            "BLOCK_DISPLAY",
            "CHEST_BOAT",
            "DRAGON_FIREBALL",
            "DROPPED_ITEM",
            "EGG",
            "ENDER_CRYSTAL",
            "ENDER_PEARL",
            "ENDER_SIGNAL",
            "EVOKER_FANGS",
            "EXPERIENCE_ORB",
            "FALLING_BLOCK",
            "FIREWORK",
            "FISHING_HOOK",
            "GIANT",
            "INTERACTION",
            "ITEM_DISPLAY",
            "ITEM_FRAME",
            "LEASH_HITCH",
            "LIGHTNING",
            "LLAMA_SPIT",
            "MARKER",
            "MINECART",
            "MINECART_CHEST",
            "MINECART_COMMAND",
            "MINECART_FURNACE",
            "MINECART_HOPPER",
            "MINECART_MOB_SPAWNER",
            "MINECART_TNT",
            "PAINTING",
            "PLAYER",
            "PRIMED_TNT",
            "SHULKER_BULLET",
            "SMALL_FIREBALL",
            "SPECTRAL_ARROW",
            "SPLASH_POTION",
            "TEXT_DISPLAY",
            "THROWN_EXP_BOTTLE",
            "TRIDENT",
            "UNKNOWN",
            "BOAT",
            "FIREBALL",
            "GLOW_ITEM_FRAME",
            "TROPICAL_FISH",
            "WIND_CHARGE",
            "WITHER_SKULL"
        )
    }
}