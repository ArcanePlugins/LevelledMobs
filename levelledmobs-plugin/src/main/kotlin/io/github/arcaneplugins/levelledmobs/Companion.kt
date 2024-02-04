package io.github.arcaneplugins.levelledmobs

import io.github.arcaneplugins.levelledmobs.compatibility.Compat119
import io.github.arcaneplugins.levelledmobs.compatibility.Compat119.getAquaticMobs
import io.github.arcaneplugins.levelledmobs.compatibility.Compat120
import io.github.arcaneplugins.levelledmobs.compatibility.Compat121
import io.github.arcaneplugins.levelledmobs.listeners.BlockPlaceListener
import io.github.arcaneplugins.levelledmobs.listeners.ChunkLoadListener
import io.github.arcaneplugins.levelledmobs.listeners.CombustListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDamageDebugListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityDamageListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityNametagListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityPickupItemListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityRegainHealthListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityTameListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityTargetListener
import io.github.arcaneplugins.levelledmobs.listeners.EntityTransformListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerDeathListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerInteractEventListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerJoinListener
import io.github.arcaneplugins.levelledmobs.listeners.PlayerPortalEventListener
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.PlaceholderApiIntegration
import io.github.arcaneplugins.levelledmobs.result.ChunkKillInfo
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.FileLoader
import io.github.arcaneplugins.levelledmobs.misc.FileLoader.loadFile
import io.github.arcaneplugins.levelledmobs.misc.FileMigrator.migrateSettingsToRules
import io.github.arcaneplugins.levelledmobs.misc.MobPluginDetection
import io.github.arcaneplugins.levelledmobs.misc.OutdatedServerVersionException
import io.github.arcaneplugins.levelledmobs.misc.VersionInfo
import io.github.arcaneplugins.levelledmobs.rules.MetricsInfo
import io.github.arcaneplugins.levelledmobs.util.UpdateChecker
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import io.github.arcaneplugins.levelledmobs.util.Utils.replaceAllInList
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerResult
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.InvalidObjectException
import java.time.Duration
import java.time.Instant
import java.util.LinkedList
import java.util.Locale
import java.util.TreeMap
import java.util.UUID
import java.util.WeakHashMap
import java.util.function.Consumer
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimpleBarChart
import org.bstats.charts.SimplePie
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

/**
 * This class contains methods used by the main class.
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
class Companion{
    private val recentlyJoinedPlayers = WeakHashMap<Player, Instant>()
    val hostileMobsGroup = mutableSetOf<EntityType>()
    val aquaticMobsGroup = mutableSetOf<EntityType>()
    val passiveMobsGroup = mutableSetOf<EntityType>()
    var updateResult = mutableListOf<String>()
    var hadRulesLoadError = false
        private set
    var useAdventure = false
    var reloadSender: CommandSender? = null
    var hasFinishedLoading = false
    var showCustomDrops = false
    val mobPluginDetectionDefinitions: MutableMap<String, MobPluginDetection> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    private val entityDeathInChunkCounter = mutableMapOf<Long, MutableMap<EntityType, ChunkKillInfo>>()
    private val chunkKillNoticationTracker = mutableMapOf<Long, MutableMap<UUID, Instant>>()
    private val playerNetherPortals = mutableMapOf<Player, Location>()
    private val playerWorldPortals = mutableMapOf<Player, Location>()
    val spawnerCopyIds = mutableListOf<UUID>()
    val spawnerInfoIds = mutableListOf<UUID>()
    var excludePlayersInCreative = false
    private val pluginManager = Bukkit.getPluginManager()
    private val metricsInfo = MetricsInfo()
    val externalCompatibilityManager = ExternalCompatibilityManager()
    private var hashMapCleanUp: SchedulerResult? = null
    private val playerLogonTimesLock = Any()
    private val playerNetherPortalsLock = Any()
    private val entityDeathInChunkCounterLock = Any()
    private val entityDeathInChunkNotifierLock = Any()

    fun load(){
        buildUniversalGroups()
    }

    private fun getSettingsVersion(): Int {
        val main = LevelledMobs.instance
        val file = File(main.dataFolder, "settings.yml")
        if (!file.exists()) {
            return 0
        }

        main.helperSettings.cs = YamlConfiguration.loadConfiguration(file)
        return main.helperSettings.getInt("file-version")
    }

    // Note: also called by the reload subcommand.
    fun loadFiles(isReload: Boolean): Boolean {
        Utils.logger.info("&fFile Loader: &7Loading files...")
        val main = LevelledMobs.instance

        val rulesFile = loadFile(
            main, "rules",
            FileLoader.RULES_FILE_VERSION
        )
        this.hadRulesLoadError = rulesFile == null
        main.rulesParsingManager.parseRulesMain(rulesFile)

        main.configUtils.playerLevellingEnabled = main.rulesManager.isPlayerLevellingEnabled()

        val settingsVersion = getSettingsVersion()
        if (settingsVersion in 21..29
        ) { // anything older than 2.0 will not be migrated
            migrateSettingsToRules()
        }

        val configLoad = loadFile(main, "settings", FileLoader.SETTINGS_FILE_VERSION)

        if (configLoad != null) // only load if settings were loaded successfully
        {
            main.helperSettings.cs = configLoad
            main.messagesCfg = loadFile(
                main, "messages",
                FileLoader.MESSAGES_FILE_VERSION
            )!!
        } else {
            // had an issue reading the file.  Disable the plugin now
            return false
        }

        main.customDropsHandler.load()

        if (!isReload) {
            //main.dropsCfg = loadEmbeddedResource("defaultDrops.yml")!!

            // remove legacy files if they exist
            val legacyFile = arrayOf("attributes.yml", "drops.yml")
            for (lFile in legacyFile) {
                val delFile = File(main.dataFolder, lFile)
                try {
                    if (delFile.exists()) {
                        delFile.delete()
                    }
                } catch (e: Exception) {
                    Utils.logger.warning("Unable to delete file " + lFile + ", " + e.message)
                }
            }

            parseDebugsEnabled()
        } else {
            // if not reloading then it is called from the server load event to make sure any dependent
            // plugins are already loaded
            parseDebugsEnabled()
            main.customDropsHandler.customDropsParser.loadDrops(
                loadFile(main, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
            )
        }

        main.configUtils.load()
        main.playerLevellingMinRelevelTime = main.helperSettings.getIntTimeUnitMS(
            "player-levelling-relevel-min-time", 5000L
        )!!
        this.useAdventure = main.helperSettings.getBoolean( "use-adventure", true)
        this.excludePlayersInCreative = main.helperSettings.getBoolean("exclude-players-in-creative")
        parseMobPluginDetection()

        return true
    }

    private fun parseDebugsEnabled() {
        val main = LevelledMobs.instance
        val debugsEnabled = main.helperSettings.cs.getStringList("debug-misc")

        if (debugsEnabled.isEmpty()) {
            return
        }

        var useAllDebugs = false
        var addedDebugs = false
        for (debug in debugsEnabled) {
            if (debug.isNullOrEmpty()) {
                continue
            }

            if ("*".equals(debug, ignoreCase = true)) {
                useAllDebugs = true
                continue
            }

            try {
                val debugType = DebugType.valueOf(debug.uppercase(Locale.getDefault()))
                main.debugManager.filterDebugTypes.add(debugType)
                addedDebugs = true
            } catch (ignored: java.lang.Exception) {
                Utils.logger.warning("Invalid value for debug-misc: $debug")
            }
        }

        if (useAllDebugs) {
            main.debugManager.filterDebugTypes.clear()
        }

        if (addedDebugs && !main.debugManager.isEnabled) {
            val useSender = if (this.reloadSender != null) this.reloadSender else Bukkit.getConsoleSender()
            main.debugManager.enableDebug(useSender!!, usetimer = false, bypassFilters = false)
            useSender.sendMessage(main.debugManager.getDebugStatus())
        }

        this.showCustomDrops = main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)
    }


    private fun parseMobPluginDetection(){
        val settings = LevelledMobs.instance.helperSettings
        val root = settings.cs.get("mob-plugin-detection") ?: return
        val cs = root as ConfigurationSection

        for (key in cs.getKeys(false)){
            val csKey = cs.get(key) ?: continue
            if (csKey !is ConfigurationSection) continue

            val pluginName = csKey.getString("plugin-name")
            val keyName = csKey.getString("key-name")
            val requirementStr = csKey.getString("requirement")

            if (pluginName.isNullOrEmpty()) continue
            if (keyName.isNullOrEmpty()){
                Utils.logger.warning("no key-name was supplied for $pluginName")
                continue
            }
            var requirement = MobPluginDetection.RequirementTypes.EXISTS
            if (!requirementStr.isNullOrEmpty()){
                try{
                    requirement = MobPluginDetection.RequirementTypes.valueOf(requirementStr.uppercase())
                }
                catch (ignored: Exception){
                    Utils.logger.warning("Invalid value: $requirementStr")
                }
            }

            val mpd = MobPluginDetection(pluginName, keyName, requirement)
            mpd.requirementValue = csKey.getString("requirement-value")
            this.mobPluginDetectionDefinitions[mpd.pluginName] = mpd
        }

        Utils.logger.info("plugins: $mobPluginDetectionDefinitions")
    }

    private fun loadEmbeddedResource(filename: String): YamlConfiguration? {
        var result: YamlConfiguration? = null
        val inputStream = LevelledMobs.instance.getResource(filename) ?: return null

        try {
            val reader = InputStreamReader(inputStream)
            result = YamlConfiguration.loadConfiguration(reader)
            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            Utils.logger.error("Error reading embedded file: " + filename + ", " + e.message)
        }

        return result
    }

    fun registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...")

        val main = LevelledMobs.instance
        main.levelManager.load()
        main.mobsQueueManager.start()
        main.nametagQueueManager.start()
        main.levelManager.entitySpawnListener.processMobSpawns = main.helperSettings.getBoolean(
            "level-mobs-upon-spawn", true
        )
        main.entityDamageDebugListener = EntityDamageDebugListener()
        main.blockPlaceListener = BlockPlaceListener()

        if (main.helperSettings.getBoolean( "debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            main.configUtils.debugEntityDamageWasEnabled = true
            pluginManager.registerEvents(main.entityDamageDebugListener, main)
        }

        pluginManager.registerEvents(main.levelManager.entitySpawnListener, main)
        pluginManager.registerEvents(EntityDamageListener(), main)
        pluginManager.registerEvents(main.entityDeathListener, main)
        pluginManager.registerEvents(EntityRegainHealthListener(), main)
        pluginManager.registerEvents(EntityTransformListener(), main)
        pluginManager.registerEvents(EntityNametagListener(), main)
        pluginManager.registerEvents(EntityTargetListener(), main)
        pluginManager.registerEvents(PlayerJoinListener(), main)
        pluginManager.registerEvents(EntityTameListener(), main)
        pluginManager.registerEvents(PlayerDeathListener(), main)
        pluginManager.registerEvents(CombustListener(), main)
        pluginManager.registerEvents(main.blockPlaceListener, main)
        pluginManager.registerEvents(PlayerPortalEventListener(), main)
        pluginManager.registerEvents(EntityPickupItemListener(), main)
        main.chunkLoadListener = ChunkLoadListener()
        main.playerInteractEventListener = PlayerInteractEventListener()
        pluginManager.registerEvents(main.playerInteractEventListener, main)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            main.placeholderApiIntegration = PlaceholderApiIntegration()
            main.placeholderApiIntegration!!.register()
        }

        if (main.helperSettings.getBoolean(
                "ensure-mobs-are-levelled-on-chunk-load", true
            )
        ) {
            pluginManager.registerEvents(main.chunkLoadListener, main)
        }
    }

    fun registerCommands(){
        Utils.logger.info("&fCommands: &7Registering commands...")

        val levelledMobsCommand = LevelledMobs.instance.getCommand("levelledmobs")
        if (levelledMobsCommand == null) {
            Utils.logger.error(
                "Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?"
            )
        } else {
            levelledMobsCommand.setExecutor(LevelledMobs.instance.levelledMobsCommand)
        }
    }

    fun setupMetrics() {
        val metrics = Metrics(LevelledMobs.instance, 6269)

        metrics.addCustomChart(SimplePie("maxlevel_used") { metricsInfo.getMaxLevelRange() })
        metrics.addCustomChart(SimplePie("custom_rules_used") { metricsInfo.getCustomRulesUsed() })
        metrics.addCustomChart(
            SimplePie("custom_drops_enabled", metricsInfo::getUsesCustomDrops)
        )
        metrics.addCustomChart(
            SimplePie("health_indicator_enabled") { metricsInfo.getUsesHealthIndicator() })
        metrics.addCustomChart(
            SimplePie("levelling_strategy") { metricsInfo.getLevellingStrategy() })
        metrics.addCustomChart(
            SimplePie("autoupdate_checker_enabled") { metricsInfo.usesAutoUpdateChecker() })
        metrics.addCustomChart(
            SimplePie("level_mobs_upon_spawn") { metricsInfo.levelMobsUponSpawn() })
        metrics.addCustomChart(
            SimplePie("check_mobs_on_chunk_load") { metricsInfo.checkMobsOnChunkLoad() })
        metrics.addCustomChart(
            SimplePie("custom-entity-names") { metricsInfo.customEntityNamesCount() })
        metrics.addCustomChart(SimplePie("utilizes-nbtdata") { metricsInfo.usesNbtData() })
        metrics.addCustomChart(
            SimplePie("utilizes_player_levelling") { metricsInfo.usesPlayerLevelling() })
        metrics.addCustomChart(SimplePie("nametag_visibility") { metricsInfo.nametagVisibility() })
        metrics.addCustomChart(
            SimpleBarChart(
                "enabled-compatibility"
            ) { metricsInfo.enabledCompats() })
    }

    fun startCleanupTask() {
        val scheduler = SchedulerWrapper {
            synchronized(entityDeathInChunkCounterLock) {
                chunkKillLimitCleanup()
            }
            synchronized(entityDeathInChunkNotifierLock) {
                chunkKillNoticationCleanup()
            }
        }

        this.hashMapCleanUp = scheduler.runTaskTimerAsynchronously(5000, 2000)
    }

    private fun chunkKillLimitCleanup() {
        val chunkKeysToRemove: MutableList<Long> = LinkedList()

        for (chunkKey in entityDeathInChunkCounter.keys) {
            //                                 Cooldown time, entity counts
            val pairList = entityDeathInChunkCounter[chunkKey] ?: continue

            val now = Instant.now()

            for (entityType in pairList.keys) {
                val chunkKillInfo = pairList[entityType]

                (chunkKillInfo!!.entrySet as MutableSet).removeIf { e: Map.Entry<Instant, Int> ->
                    e.key < now.minusSeconds(e.value.toLong())
                }
            }

            pairList.entries.removeIf { e: Map.Entry<EntityType, ChunkKillInfo> -> e.value.isEmpty }

            if (pairList.isEmpty()) {
                // Remove the object to prevent iterate over exceed amount of empty pairList
                chunkKeysToRemove.add(chunkKey)
            }
        }

        for (chunkKey in chunkKeysToRemove) {
            entityDeathInChunkCounter.remove(chunkKey)
        }
    }

    private fun chunkKillNoticationCleanup() {
        val iterator = chunkKillNoticationTracker.keys.iterator()

        while (iterator.hasNext()) {
            val chunkKey = iterator.next()
            val playerTimestamps: MutableMap<UUID, Instant> =
                chunkKillNoticationTracker[chunkKey]!!
            playerTimestamps.entries
                .removeIf { e: Map.Entry<UUID, Instant> ->
                    Duration.between(
                        e.value,
                        Instant.now()
                    ).toSeconds() > 30L
                }

            if (playerTimestamps.isEmpty()) {
                iterator.remove()
            }
        }
    }

    fun getorAddPairForSpecifiedChunk(
        chunkKey: Long
    ): MutableMap<EntityType, ChunkKillInfo> {
        synchronized(entityDeathInChunkCounterLock) {
            return entityDeathInChunkCounter.computeIfAbsent(chunkKey) {
                mutableMapOf()
            }
        }
    }

    fun getorAddPairForSpecifiedChunks(
        chunkKeys: List<Long>
    ): List<Map<EntityType, ChunkKillInfo>> {
        val results = mutableListOf<Map<EntityType, ChunkKillInfo>>()

        synchronized(entityDeathInChunkCounterLock) {
            for (chunkKey in chunkKeys) {
                results.add(
                    entityDeathInChunkCounter.computeIfAbsent(chunkKey) {
                        mutableMapOf()
                    }
                )
            }
        }

        return results
    }

    fun doesUserHaveCooldown(
        chunkKeys: List<Long>,
        userId: UUID
    ): Boolean {
        val chunkInfos = mutableListOf<MutableMap<UUID, Instant>>()

        synchronized(entityDeathInChunkNotifierLock) {
            for (chunkKey in chunkKeys) {
                if (chunkKillNoticationTracker.containsKey(chunkKey)) {
                    chunkInfos.add(chunkKillNoticationTracker[chunkKey]!!)
                }
            }
        }

        if (chunkInfos.isEmpty()) {
            return false
        }

        for (chunkInfo in chunkInfos) {
            if (!chunkInfo.containsKey(userId)) {
                continue
            }
            val instant = chunkInfo[userId]
            if (Duration.between(instant, Instant.now()).toSeconds() <= 30L) {
                return true
            }
        }

        return false
    }

    fun addUserCooldown(
        chunkKeys: MutableList<Long>,
        userId: UUID
    ) {
        synchronized(entityDeathInChunkNotifierLock) {
            for (chunkKey in chunkKeys) {
                val entry =
                    chunkKillNoticationTracker.computeIfAbsent(chunkKey) {
                        _: Long -> mutableMapOf()
                    }
                entry[userId] = Instant.now()
            }
        }
    }

    fun clearChunkKillCache() {
        synchronized(entityDeathInChunkCounterLock) {
            entityDeathInChunkCounter.clear()
        }
        synchronized(entityDeathInChunkNotifierLock) {
            chunkKillNoticationTracker.clear()
        }
    }

    //Check for updates on the Spigot page.
    fun checkUpdates() {
        val main = LevelledMobs.instance
        if (main.helperSettings.getBoolean("use-update-checker", true)) {
            val updateChecker = UpdateChecker(main, 74304)
            try {
                updateChecker.getLatestVersion { latestVersion: String ->
                    val currentVersion =
                        updateChecker.currentVersion.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                    val thisVersion: VersionInfo
                    val spigotVersion: VersionInfo
                    var isOutOfDate: Boolean
                    var isNewerVersion: Boolean

                    try {
                        thisVersion = VersionInfo(currentVersion)
                        spigotVersion = VersionInfo(latestVersion)

                        isOutOfDate = (thisVersion < spigotVersion)
                        isNewerVersion = (thisVersion > spigotVersion)
                    } catch (e: InvalidObjectException) {
                        Utils.logger.warning(
                            "Got exception creating version objects: " + e.message
                        )

                        isOutOfDate = currentVersion != latestVersion
                        isNewerVersion = currentVersion.contains("indev")
                    }
                    if (isNewerVersion) {
                        updateResult = mutableListOf(
                            "&7Your &bLevelledMobs&7 version is &ba pre-release&7. Latest release version is &bv%latestVersion%&7. &8(&7You're running &bv%currentVersion%&8)"
                        )

                        updateResult = replaceAllInList(
                            updateResult, "%currentVersion%", currentVersion
                        )
                        updateResult = replaceAllInList(
                            updateResult, "%latestVersion%", latestVersion
                        )
                        updateResult = colorizeAllInList(updateResult)

                        updateResult.forEach(Consumer { message: String ->
                            Utils.logger.warning(message)
                        })
                    } else if (isOutOfDate) {
                        // for some reason config#getStringList doesn't allow defaults??

                        updateResult = if (main.messagesCfg.contains("other.update-notice.messages")) {
                            main.messagesCfg.getStringList(
                                "other.update-notice.messages"
                            )
                        } else {
                            mutableListOf(
                                "&b&nLevelledMobs Update Checker Notice:",
                                "&7Your &bLevelledMobs&7 version is &boutdated&7! Please update to" +
                                    "&bv%latestVersion%&7 as soon as possible. &8(&7You''re running &bv%currentVersion%&8)"
                            )
                        }

                        updateResult = replaceAllInList(
                            updateResult, "%currentVersion%", currentVersion
                        )
                        updateResult = replaceAllInList(
                            updateResult, "%latestVersion%", latestVersion
                        )
                        updateResult = colorizeAllInList(updateResult)

                        if (main.messagesCfg.getBoolean(
                                "other.update-notice.send-in-console",
                                true
                            )
                        ) {
                            updateResult.forEach(Consumer { message: String ->
                                Utils.logger.warning(message)
                            })
                        }

                        // notify any players that may be online already
                        if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true)) {
                            Bukkit.getOnlinePlayers().forEach { onlinePlayer: Player? ->
                                if (onlinePlayer!!.hasPermission(
                                        "levelledmobs.receive-update-notifications"
                                    )
                                ) {
                                    for (msg in updateResult) {
                                        onlinePlayer.sendMessage(msg)
                                    }
                                    //updateResult.forEach(onlinePlayer::sendMessage); //compiler didn't like this :(
                                }
                            }
                        }
                    }
                }
            } catch (e: OutdatedServerVersionException) {
                e.printStackTrace()
            }
        }
    }

    fun shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...")
        val main = LevelledMobs.instance
        main.mobsQueueManager.stop()
        main.nametagQueueManager.stop()
        hashMapCleanUp?.cancelTask()
        if (!main.ver.isRunningFolia) {
            Bukkit.getScheduler().cancelTasks(main)
        }
    }

    private fun buildUniversalGroups() {
        val versionInfo = LevelledMobs.instance.ver

        // include interfaces: Monster, Boss
        hostileMobsGroup.addAll(mutableListOf(
            EntityType.ENDER_DRAGON,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.PHANTOM,
            EntityType.SHULKER,
            EntityType.SLIME
        ))

        // include interfaces: Animals, WaterMob
        passiveMobsGroup.addAll(mutableListOf(
            EntityType.IRON_GOLEM,
            EntityType.SNOWMAN
        ))

        if (versionInfo.majorVersion >= 1.19) {
            passiveMobsGroup.addAll(Compat119.getPassiveMobs())
        }
        if (versionInfo.majorVersion >= 1.20) {
            passiveMobsGroup.addAll(Compat120.getPassiveMobs())
        }
        if (versionInfo.majorVersion >= 1.21) {
            passiveMobsGroup.addAll(Compat121.getPassiveMobs())
        }

        if (versionInfo.majorVersion >= 1.19) {
            hostileMobsGroup.addAll(Compat119.getHostileMobs())
        }

        // include interfaces: WaterMob
        aquaticMobsGroup.addAll(mutableListOf(
            EntityType.DROWNED,
            EntityType.ELDER_GUARDIAN,
            EntityType.GUARDIAN,
            EntityType.TURTLE
        ))

        if (versionInfo.majorVersion >= 1.19) {
            aquaticMobsGroup.addAll(getAquaticMobs())
        }
    }

    fun addRecentlyJoinedPlayer(player: Player?) {
        synchronized(playerLogonTimesLock) {
            recentlyJoinedPlayers.put(player, Instant.now())
        }
    }

    fun getRecentlyJoinedPlayerLogonTime(player: Player?): Instant? {
        synchronized(playerLogonTimesLock) {
            return recentlyJoinedPlayers[player]
        }
    }

    fun removeRecentlyJoinedPlayer(player: Player?) {
        synchronized(playerLogonTimesLock) {
            recentlyJoinedPlayers.remove(player)
        }
    }

    fun getPlayerNetherPortalLocation(player: Player): Location? {
        synchronized(playerNetherPortalsLock) {
            return playerNetherPortals[player]
        }
    }

    fun setPlayerNetherPortalLocation(
        player: Player,
        location: Location?
    ) {
        synchronized(playerNetherPortalsLock) {
            playerNetherPortals.put(
                player,
                location!!
            )
        }
    }

    fun getPlayerWorldPortalLocation(player: Player): Location? {
        synchronized(playerNetherPortalsLock) {
            return playerWorldPortals[player]
        }
    }

    fun setPlayerWorldPortalLocation(
        player: Player,
        location: Location?
    ) {
        synchronized(playerNetherPortalsLock) {
            playerWorldPortals.put(
                player,
                location!!
            )
        }
    }
}