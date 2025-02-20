package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.commands.CommandHandler
import io.github.arcaneplugins.levelledmobs.customdrops.CustomDropsHandler
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.listeners.BlockPlaceListener
import io.github.arcaneplugins.levelledmobs.listeners.ChunkLoadListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDamageDebugListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDamageListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDeathListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityTransformListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerDeathListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerInteractEventListener
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.managers.MobsQueueManager
import io.github.arcaneplugins.levelledmobs.managers.NametagQueueManager
import io.github.arcaneplugins.levelledmobs.managers.NotifyManager
import io.github.arcaneplugins.levelledmobs.managers.PlaceholderApiIntegration
import io.github.arcaneplugins.levelledmobs.misc.NametagTimerChecker
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.nametag.Definitions
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.rules.RulesParser
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.util.ConfigUtils
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils
import io.github.arcaneplugins.levelledmobs.util.QuickTimer
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.time.Instant
import java.util.Random
import java.util.Stack
import java.util.WeakHashMap
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin


/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and
 * onDisable on shutdown.
 *
 * @author lokka30, stumper66
 * @since 1.0
 */
class LevelledMobs : JavaPlugin() {
    val levelInterface: LevelInterface2 = LevelManager()
    val levelManager = LevelManager()
    val mobDataManager = MobDataManager()
    val customDropsHandler = CustomDropsHandler()
    val chunkLoadListener = ChunkLoadListener()
    val blockPlaceListener = BlockPlaceListener()
    val playerInteractEventListener = PlayerInteractEventListener()
    val playerDeathListener = PlayerDeathListener()
    val entityDamageListener = EntityDamageListener()
    val entityTransformListener = EntityTransformListener()
    val entityDeathListener = EntityDeathListener()
    val mainCompanion = MainCompanion()
    val rulesParsingManager = RulesParser()
    val rulesManager = RulesManager()
    val mobsQueueManager = MobsQueueManager()
    val nametagQueueManager = NametagQueueManager()
    val nametagTimerChecker = NametagTimerChecker()
    val attributeSyncObject = Any()
    val random = Random()
    var placeholderApiIntegration: PlaceholderApiIntegration? = null
        internal set
    val helperSettings = YmlParsingHelper(YamlConfiguration())
    var playerLevellingMinRelevelTime = 0L
        internal set
    var maxPlayersRecorded = 0
    val debugManager = DebugManager()
    val definitions = Definitions()
    val ver = ServerVersionInfo()

    // Configuration
    var messagesCfg = YamlConfiguration()
        internal set
    val configUtils = ConfigUtils()

    // Misc
    val customMobGroups = mutableMapOf<String, MutableSet<String>>()
    var entityDamageDebugListener = EntityDamageDebugListener()
    private var loadTime = 0L
    val playerLevellingEntities = WeakHashMap<LivingEntity, Instant>()
    var cacheCheck: Stack<LivingEntityWrapper>? = null

    companion object {
        @JvmStatic
        lateinit var instance: LevelledMobs
            private set
    }

    override fun onLoad() {
        instance = this
        CommandHandler.load(CommandHandler.LoadingStage.ON_LOAD)
    }

    override fun onEnable() {
        val timer = QuickTimer()

        CommandHandler.load(CommandHandler.LoadingStage.ON_ENABLE)
        this.ver.load()
        if (ver.minecraftVersion <= 1.18){
            Log.sev("This minecraft version is NOT supported. Use at your own risk!")
        }
        this.definitions.load()
        this.nametagQueueManager.load()
        this.mainCompanion.load()
        (this.levelInterface as LevelManager).load()
        if (!mainCompanion.loadFiles()) {
            // had fatal error reading required files
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        definitions.useTranslationComponents = helperSettings.getBoolean(
            "use-translation-components", true
        )
        definitions.setUseLegacySerializer(
            helperSettings.getBoolean("use-legacy-serializer", true)
        )
        nametagQueueManager.nametagSenderHandler.refresh()
        mainCompanion.registerListeners()

        Log.inf("Running misc procedures")
        if (nametagQueueManager.hasNametagSupport) {
            levelManager.startNametagAutoUpdateTask()
            levelManager.startNametagTimer()

            if (!ver.isRunningFolia){
                val scheduler = SchedulerWrapper {
                    nametagQueueManager.taskChecker()
                    mobsQueueManager.taskChecker()
                }
                scheduler.runTaskTimerAsynchronously(50000, 5000)
            }
        }

        prepareToLoadCustomDrops()
        mainCompanion.checkSettingsWithMaxPlayerOptions()
        mainCompanion.startCleanupTask()
        mainCompanion.setupMetrics()
        mainCompanion.checkUpdates()

        loadTime += timer.timer
        Log.inf("Start-up complete (took ${loadTime}ms)")
    }

    override fun onDisable() {
        val disableTimer = QuickTimer()
        disableTimer.start()

        CommandHandler.load(CommandHandler.LoadingStage.ON_DISABLE)
        levelManager.stopNametagAutoUpdateTask()
        mainCompanion.shutDownAsyncTasks()

        Log.inf("Shut-down complete (took ${disableTimer.timer}ms)")
    }

    private fun prepareToLoadCustomDrops(){
        if (Bukkit.getPluginManager().getPlugin("LM_Items") == null &&
            mainCompanion.showCustomDrops) {
            customDropsHandler.customDropsParser.showCustomDropsDebugInfo(null)
        }
    }

    fun reloadLM(sender: CommandSender) {
        NotifyManager.clearLastError()
        customDropsHandler.customDropsParser.invalidExternalItems.clear()
        var reloadStartedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.started"
        )
        reloadStartedMsg = Utils.replaceAllInList(
            reloadStartedMsg, "%prefix%",
            configUtils.prefix
        )
        reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg)
        reloadStartedMsg.forEach(Consumer { s: String ->
            sender.sendMessage(s)
        })

        mainCompanion.reloadSender = sender
        mainCompanion.loadFiles()
        mainCompanion.checkListenersWithVariablePriorities()
        chunkLoadListener.load()
        RandomLevellingStrategy.clearCache()

        var reloadFinishedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.finished"
        )
        reloadFinishedMsg = Utils.replaceAllInList(
            reloadFinishedMsg, "%prefix%",
            configUtils.prefix
        )
        reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg)

        if (helperSettings.getBoolean("ensure-mobs-are-levelled-on-chunk-load")
            && !configUtils.chunkLoadListenerWasEnabled
        ) {
            configUtils.chunkLoadListenerWasEnabled = true
            Bukkit.getPluginManager().registerEvents(chunkLoadListener, this)
        } else if (!helperSettings.getBoolean( "ensure-mobs-are-levelled-on-chunk-load")
            && configUtils.chunkLoadListenerWasEnabled
        ) {
            configUtils.chunkLoadListenerWasEnabled = false
            HandlerList.unregisterAll(chunkLoadListener)
        }

        if (customDropsHandler.customDropsParser.hadParsingError && sender is Player){
            sender.sendMessage(MessageUtils.colorizeAll(
                "&b&lLevelledMobs:&r &6There was an error parsing customdrops.yml&r\n" +
                "Check the console log for more details"
            ))
        }

        rulesManager.clearTempDisabledRulesCounts()
        definitions.useTranslationComponents = helperSettings.getBoolean(
            "use-translation-components", true
        )
        definitions.setUseLegacySerializer(
            helperSettings.getBoolean(
                "use-legacy-serializer", true
            )
        )
        mainCompanion.checkSettingsWithMaxPlayerOptions()
        nametagQueueManager.nametagSenderHandler.refresh()

        reloadFinishedMsg.forEach(Consumer { s: String ->
            sender.sendMessage(s)
        })
    }
}