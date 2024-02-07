package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.commands.LevelledMobsCommand
import io.github.arcaneplugins.levelledmobs.customdrops.CustomDropsHandler
import io.github.arcaneplugins.levelledmobs.listeners.BlockPlaceListener
import io.github.arcaneplugins.levelledmobs.listeners.ChunkLoadListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDamageDebugListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDeathListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerInteractEventListener
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.managers.MobsQueueManager
import io.github.arcaneplugins.levelledmobs.managers.NametagQueueManager
import io.github.arcaneplugins.levelledmobs.managers.PlaceholderApiIntegration
import io.github.arcaneplugins.levelledmobs.misc.FileLoader
import io.github.arcaneplugins.levelledmobs.misc.NametagTimerChecker
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.nametag.Definitions
import io.github.arcaneplugins.levelledmobs.nametag.ServerVersionInfo
import io.github.arcaneplugins.levelledmobs.rules.RulesManager
import io.github.arcaneplugins.levelledmobs.rules.RulesParsingManager
import io.github.arcaneplugins.levelledmobs.util.ConfigUtils
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
    var levelManager = LevelManager()
    val mobDataManager = MobDataManager()
    var customDropsHandler = CustomDropsHandler()
    var chunkLoadListener = ChunkLoadListener()
    var blockPlaceListener = BlockPlaceListener()
    var playerInteractEventListener = PlayerInteractEventListener()
    val entityDeathListener = EntityDeathListener()
    val mainCompanion = MainCompanion()
    val rulesParsingManager = RulesParsingManager()
    val rulesManager = RulesManager()
    val mobsQueueManager = MobsQueueManager()
    val nametagQueueManager = NametagQueueManager()
    val nametagTimerChecker = NametagTimerChecker()
    val attributeSyncObject = Any()
    val levelledMobsCommand = LevelledMobsCommand()
    val random = Random()
    var placeholderApiIntegration: PlaceholderApiIntegration? = null
        internal set
    var migratedFromPre30 = false
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
    var dropsCfg = YamlConfiguration()
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
    }

    override fun onEnable() {
        val timer = QuickTimer()

        this.ver.load()
        if (ver.minecraftVersion <= 1.18){
            Utils.logger.error("This minecraft version is NOT supported. Use at your own risk!")
        }
        this.definitions.load()
        this.nametagQueueManager.load()
        this.mainCompanion.load()
        (this.levelInterface as LevelManager).load()
        if (!mainCompanion.loadFiles(false)) {
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
        mainCompanion.registerCommands()

        Utils.logger.info("Running misc procedures")
        if (nametagQueueManager.hasNametagSupport) {
            levelManager.startNametagAutoUpdateTask()
            levelManager.startNametagTimer()
        }

        prepareToLoadCustomDrops()
        mainCompanion.startCleanupTask()
        mainCompanion.setupMetrics()
        mainCompanion.checkUpdates()

        loadTime += timer.timer
        Utils.logger.info("Start-up complete (took ${loadTime}ms)")
    }

    override fun onDisable() {
        val disableTimer = QuickTimer()
        disableTimer.start()

        levelManager.stopNametagAutoUpdateTask()
        mainCompanion.shutDownAsyncTasks()

        Utils.logger.info("Shut-down complete (took ${disableTimer.timer}ms)")
    }

    private fun prepareToLoadCustomDrops(){
        if (Bukkit.getPluginManager().getPlugin("LM_Items") != null) {
            val mainInstance = this
            val wrapper = SchedulerWrapper {
                customDropsHandler.customDropsParser.loadDrops(
                    FileLoader.loadFile(mainInstance, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
                )
                mainCompanion.hasFinishedLoading = true
                if (mainCompanion.showCustomDrops) customDropsHandler.customDropsParser.showCustomDropsDebugInfo(null)
            }
            wrapper.runDelayed(10L)
        } else {
            customDropsHandler.customDropsParser.loadDrops(
                FileLoader.loadFile(this, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
            )
            if (mainCompanion.showCustomDrops) customDropsHandler.customDropsParser.showCustomDropsDebugInfo(null)
        }
    }

    fun reloadLM(sender: CommandSender) {
        migratedFromPre30 = false
        customDropsHandler.customDropsParser.invalidExternalItems.clear()
        var reloadStartedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.started"
        )
        reloadStartedMsg = Utils.replaceAllInList(
            reloadStartedMsg, "%prefix%",
            configUtils.prefix
        )
        reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg)
        reloadStartedMsg.forEach(Consumer { s: String? ->
            sender.sendMessage(
                s!!
            )
        })

        mainCompanion.reloadSender = sender
        mainCompanion.loadFiles(true)

        var reloadFinishedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.finished"
        )
        reloadFinishedMsg = Utils.replaceAllInList(
            reloadFinishedMsg, "%prefix%",
            configUtils.prefix
        )
        reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg)

        if (helperSettings.getBoolean( "debug-entity-damage")
            && !configUtils.debugEntityDamageWasEnabled
        ) {
            configUtils.debugEntityDamageWasEnabled = true
            Bukkit.getPluginManager().registerEvents(entityDamageDebugListener, this)
        } else if (!helperSettings.getBoolean( "debug-entity-damage")
            && configUtils.debugEntityDamageWasEnabled
        ) {
            configUtils.debugEntityDamageWasEnabled = false
            HandlerList.unregisterAll(entityDamageDebugListener)
        }

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

        levelManager.entitySpawnListener.processMobSpawns = helperSettings.getBoolean(
            "level-mobs-upon-spawn", true
        )

        levelManager.clearRandomLevellingCache()
        configUtils.playerLevellingEnabled = rulesManager.isPlayerLevellingEnabled()
        rulesManager.clearTempDisabledRulesCounts()
        definitions.useTranslationComponents = helperSettings.getBoolean(
            "use-translation-components", true
        )
        definitions.setUseLegacySerializer(
            helperSettings.getBoolean(
                "use-legacy-serializer", true
            )
        )
        nametagQueueManager.nametagSenderHandler.refresh()

        reloadFinishedMsg.forEach(Consumer { s: String ->
            sender.sendMessage(s)
        })
    }
}