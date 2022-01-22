/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import io.github.geniot.indexedtreemap.IndexedNavigableMap;
import io.github.geniot.indexedtreemap.IndexedNavigableSet;
import io.github.geniot.indexedtreemap.IndexedTreeSet;
import me.lokka30.levelledmobs.commands.LevelledMobsCommand;
import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.BlockPlaceListener;
import me.lokka30.levelledmobs.listeners.ChunkLoadListener;
import me.lokka30.levelledmobs.listeners.EntityDamageDebugListener;
import me.lokka30.levelledmobs.listeners.PlayerInteractEventListener;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.MobDataManager;
import me.lokka30.levelledmobs.managers.MobHeadManager;
import me.lokka30.levelledmobs.managers.MobsQueueManager;
import me.lokka30.levelledmobs.managers.NametagQueueManager;
import me.lokka30.levelledmobs.managers.PlaceholderApiIntegration;
import me.lokka30.levelledmobs.misc.ConfigUtils;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Namespaced_Keys;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.rules.RulesManager;
import me.lokka30.levelledmobs.rules.RulesParsingManager;
import me.lokka30.microlib.maths.QuickTimer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and onDisable on shutdown.
 *
 * @author lokka30, stumper66
 * @since 1.0
 */
public final class LevelledMobs extends JavaPlugin {

    // Manager classes
    public LevelInterface levelInterface;
    public LevelManager levelManager;
    public final MobDataManager mobDataManager = new MobDataManager(this);
    public CustomDropsHandler customDropsHandler;
    ChunkLoadListener chunkLoadListener;
    BlockPlaceListener blockPlaceListener;
    PlayerInteractEventListener playerInteractEventListener;
    public Namespaced_Keys namespaced_keys;
    public final Companion companion = new Companion(this);
    public final MobHeadManager mobHeadManager = new MobHeadManager(this);
    public final RulesParsingManager rulesParsingManager = new RulesParsingManager(this);
    public final RulesManager rulesManager = new RulesManager(this);
    public final MobsQueueManager _mobsQueueManager = new MobsQueueManager(this);
    public final NametagQueueManager nametagQueueManager_ = new NametagQueueManager(this);
    public final NametagTimerChecker nametagTimerChecker = new NametagTimerChecker(this);
    public final Object attributeSyncObject = new Object();
    public LevelledMobsCommand levelledMobsCommand;
    public Random random;
    public PlaceholderApiIntegration placeholderApiIntegration;
    public boolean migratedFromPre30;
    public YmlParsingHelper helperSettings;
    public int playerLevellingMinRelevelTime;
    public int maxPlayersRecorded;

    // Configuration
    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration attributesCfg;
    public YamlConfiguration dropsCfg;
    public final ConfigUtils configUtils = new ConfigUtils(this);

    // Misc
    public Map<String, Set<String>> customMobGroups;
    EntityDamageDebugListener entityDamageDebugListener;
    public int incompatibilitiesAmount;
    private long loadTime;
    public WeakHashMap<LivingEntity, Instant> playerLevellingEntities;
    public Stack<LivingEntityWrapper> cacheCheck;
    public HashMap<Long, IndexedTreeSet<Pair<Timestamp, LivingEntityWrapper>>> entityDeathInChunkCounter;
    public BukkitTask hashMapCleanUp;
    public float maximumCoolDownTime = 0.0F;
    public int maximumDeathInChunkThreshold = 0;

    @Override
    public void onEnable() {
        Utils.logger.info("&f~ Initiating start-up procedure ~");
        final QuickTimer timer = new QuickTimer();

        this.namespaced_keys = new Namespaced_Keys(this);
        this.playerLevellingEntities = new WeakHashMap<>();
        this.entityDeathInChunkCounter = new HashMap<>();
        this.helperSettings = new YmlParsingHelper();
        this.random = new Random();
        this.customMobGroups = new TreeMap<>();
        this.levelInterface = new LevelManager(this);
        companion.checkCompatibility();
        if (!companion.loadFiles(false)) {
            // had fatal error reading required files
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        companion.registerListeners();
        companion.registerCommands();
        companion.loadSpigotConfig();

        Utils.logger.info("&fStart-up: &7Running misc procedures...");
        if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            levelManager.startNametagAutoUpdateTask();
            levelManager.startNametagTimer();
        }
        /*
         Clean up HashMap(entityDeathInChunkCounter) every 5 minutes
         We need to do this to prevent too many killing record in the memory
         Assume there's 50 players killing mobs intensely in game, 1 mob/ 5 sec
         i.e. 60 mobs * 50 players = 3000 records maximum every five minutes
         Shouldn't freeze the server
         */
        hashMapCleanUp = new BukkitRunnable() {
            @Override
            public void run() {
                for(Map.Entry<Long, IndexedTreeSet<Pair<Timestamp, LivingEntityWrapper>>> i : entityDeathInChunkCounter.entrySet()){
                    IndexedTreeSet<Pair<Timestamp,LivingEntityWrapper>> pairList = i.getValue();
                    while(pairList != null && !pairList.isEmpty() && Math.abs(pairList.first().getKey().getTime()-
                            System.currentTimeMillis()) <= maximumCoolDownTime * 1000.0F) {
                        pairList.pollFirst();
                    }
                    if(pairList.isEmpty()){
                        pairList = null; // Remove the object to prevent iterate over exceed amount of empty pairList
                    }
                }
            }
        }.runTaskTimer(this, 0, 6000);
        companion.setupMetrics();
        companion.checkUpdates();

        loadTime += timer.getTimer();
        Utils.logger.info("&f~ Start-up complete, took &b" + loadTime + "ms&f ~");
    }

    public void reloadLM(final @NotNull CommandSender sender){
        migratedFromPre30 = false;
        List<String> reloadStartedMsg = messagesCfg.getStringList("command.levelledmobs.reload.started");
        reloadStartedMsg = Utils.replaceAllInList(reloadStartedMsg, "%prefix%", configUtils.getPrefix());
        reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg);
        reloadStartedMsg.forEach(sender::sendMessage);

        companion.loadFiles(true);

        List<String> reloadFinishedMsg = messagesCfg.getStringList("command.levelledmobs.reload.finished");
        reloadFinishedMsg = Utils.replaceAllInList(reloadFinishedMsg, "%prefix%", configUtils.getPrefix());
        reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg);

        if (ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            if (ExternalCompatibilityManager.hasProtocolLibInstalled() && (levelManager.nametagAutoUpdateTask == null || levelManager.nametagAutoUpdateTask.isCancelled()))
                levelManager.startNametagAutoUpdateTask();
            else if (!ExternalCompatibilityManager.hasProtocolLibInstalled() && levelManager.nametagAutoUpdateTask != null && !levelManager.nametagAutoUpdateTask.isCancelled())
                levelManager.stopNametagAutoUpdateTask();
        }

        if (helperSettings.getBoolean(settingsCfg,"debug-entity-damage") && !configUtils.debugEntityDamageWasEnabled) {
            configUtils.debugEntityDamageWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(entityDamageDebugListener, this);
        } else if (!helperSettings.getBoolean(settingsCfg,"debug-entity-damage") && configUtils.debugEntityDamageWasEnabled) {
            configUtils.debugEntityDamageWasEnabled = false;
            HandlerList.unregisterAll(entityDamageDebugListener);
        }

        if (helperSettings.getBoolean(settingsCfg,"ensure-mobs-are-levelled-on-chunk-load") && !configUtils.chunkLoadListenerWasEnabled) {
            configUtils.chunkLoadListenerWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(chunkLoadListener, this);
        } else if (!helperSettings.getBoolean(settingsCfg,"ensure-mobs-are-levelled-on-chunk-load") && configUtils.chunkLoadListenerWasEnabled) {
            configUtils.chunkLoadListenerWasEnabled = false;
            HandlerList.unregisterAll(chunkLoadListener);
        }

        levelManager.entitySpawnListener.processMobSpawns = helperSettings.getBoolean(settingsCfg, "level-mobs-upon-spawn", true);
        levelManager.clearRandomLevellingCache();
        configUtils.playerLevellingEnabled = rulesManager.isPlayerLevellingEnabled();

        reloadFinishedMsg.forEach(sender::sendMessage);
    }

    @Override
    public void onDisable() {
        Utils.logger.info("&f~ Initiating shut-down procedure ~");

        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();
        hashMapCleanUp.cancel();
        companion.shutDownAsyncTasks();

        Utils.logger.info("&f~ Shut-down complete, took &b" + disableTimer.getTimer() + "ms&f ~");
    }
}
