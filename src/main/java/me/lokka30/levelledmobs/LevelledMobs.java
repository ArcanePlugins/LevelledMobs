/*
 * Copyright (c) 2020-2022  lokka30
 * Use of this source code is governed by the GNU AGPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.WeakHashMap;
import me.lokka30.levelledmobs.commands.LevelledMobsCommand;
import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.BlockPlaceListener;
import me.lokka30.levelledmobs.listeners.ChunkLoadListener;
import me.lokka30.levelledmobs.listeners.EntityDamageDebugListener;
import me.lokka30.levelledmobs.listeners.PlayerInteractEventListener;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.MobDataManager;
import me.lokka30.levelledmobs.managers.MobHeadManager;
import me.lokka30.levelledmobs.managers.MobsQueueManager;
import me.lokka30.levelledmobs.managers.NametagQueueManager;
import me.lokka30.levelledmobs.managers.PlaceholderApiIntegration;
import me.lokka30.levelledmobs.misc.FileLoader;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NamespacedKeys;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.YmlParsingHelper;
import me.lokka30.levelledmobs.nametag.Definitions;
import me.lokka30.levelledmobs.nametag.ServerVersionInfo;
import me.lokka30.levelledmobs.rules.RulesManager;
import me.lokka30.levelledmobs.rules.RulesParsingManager;
import me.lokka30.levelledmobs.util.ConfigUtils;
import me.lokka30.levelledmobs.util.QuickTimer;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * This is the main class of the plugin. Bukkit will call onLoad and onEnable on startup, and
 * onDisable on shutdown.
 *
 * @author lokka30, stumper66
 * @since 1.0
 */
public final class LevelledMobs extends JavaPlugin {

    public LevelInterface levelInterface;
    public LevelManager levelManager;
    public MobDataManager mobDataManager;
    public CustomDropsHandler customDropsHandler;
    public ChunkLoadListener chunkLoadListener;
    public BlockPlaceListener blockPlaceListener;
    public PlayerInteractEventListener playerInteractEventListener;
    public NamespacedKeys namespacedKeys;
    public Companion companion;
    public MobHeadManager mobHeadManager;
    public RulesParsingManager rulesParsingManager;
    public RulesManager rulesManager;
    public MobsQueueManager mobsQueueManager;
    public NametagQueueManager nametagQueueManager;
    public NametagTimerChecker nametagTimerChecker;
    public final Object attributeSyncObject = new Object();
    public LevelledMobsCommand levelledMobsCommand;
    public Random random;
    public PlaceholderApiIntegration placeholderApiIntegration;
    public boolean migratedFromPre30;
    public YmlParsingHelper helperSettings;
    public long playerLevellingMinRelevelTime;
    public int maxPlayersRecorded;
    private Definitions definitions;
    private ServerVersionInfo ver;
    private static LevelledMobs instance;

    // Configuration
    public YamlConfiguration settingsCfg;
    public YamlConfiguration messagesCfg;
    public YamlConfiguration dropsCfg;
    public final ConfigUtils configUtils = new ConfigUtils(this);

    // Misc
    public Map<String, Set<String>> customMobGroups;
    public EntityDamageDebugListener entityDamageDebugListener;
    private long loadTime;
    public WeakHashMap<LivingEntity, Instant> playerLevellingEntities;
    public Stack<LivingEntityWrapper> cacheCheck;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        final QuickTimer timer = new QuickTimer();

        this.ver = new ServerVersionInfo();
        this.definitions = new Definitions();
        this.nametagQueueManager = new NametagQueueManager(this);
        this.mobsQueueManager = new MobsQueueManager(this);
        this.companion = new Companion(this);
        this.mobDataManager = new MobDataManager(this);
        this.mobHeadManager = new MobHeadManager(this);
        this.rulesParsingManager = new RulesParsingManager(this);
        this.rulesManager = new RulesManager(this);
        this.nametagTimerChecker = new NametagTimerChecker(this);
        this.namespacedKeys = new NamespacedKeys(this);
        this.playerLevellingEntities = new WeakHashMap<>();
        this.helperSettings = new YmlParsingHelper();
        this.random = new Random();
        this.customMobGroups = new TreeMap<>();
        this.levelInterface = new LevelManager(this);
        if (!companion.loadFiles(false)) {
            // had fatal error reading required files
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        definitions.useTranslationComponents = helperSettings.getBoolean(settingsCfg,
                "use-translation-components", true);
        definitions.setUseLegacySerializer(helperSettings.getBoolean(settingsCfg,
                "use-legacy-serializer", true));
        nametagQueueManager.nametagSenderHandler.refresh();
        companion.registerListeners();
        companion.registerCommands();

        Utils.logger.info("Running misc procedures");
        if (this.nametagQueueManager.hasNametagSupport()) {
            levelManager.startNametagAutoUpdateTask();
            levelManager.startNametagTimer();
        }

        prepareToLoadCustomDrops();
        companion.startCleanupTask();
        companion.setupMetrics();
        companion.checkUpdates();

        loadTime += timer.getTimer();
        Utils.logger.info("Start-up complete (took " + loadTime + "ms)");
    }

    private void prepareToLoadCustomDrops(){
        if (Bukkit.getPluginManager().getPlugin("LM_Items") != null){
            final LevelledMobs mainInstance = this;
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> customDropsHandler.customDropsParser.loadDrops(
                    FileLoader.loadFile(mainInstance, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
            ), 10L);
        }
        else{
            customDropsHandler.customDropsParser.loadDrops(
                    FileLoader.loadFile(this, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
            );
        }
    }

    public void reloadLM(final @NotNull CommandSender sender) {
        migratedFromPre30 = false;
        List<String> reloadStartedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.started");
        reloadStartedMsg = Utils.replaceAllInList(reloadStartedMsg, "%prefix%",
            configUtils.getPrefix());
        reloadStartedMsg = Utils.colorizeAllInList(reloadStartedMsg);
        reloadStartedMsg.forEach(sender::sendMessage);

        companion.loadFiles(true);

        List<String> reloadFinishedMsg = messagesCfg.getStringList(
            "command.levelledmobs.reload.finished");
        reloadFinishedMsg = Utils.replaceAllInList(reloadFinishedMsg, "%prefix%",
            configUtils.getPrefix());
        reloadFinishedMsg = Utils.colorizeAllInList(reloadFinishedMsg);

        if (helperSettings.getBoolean(settingsCfg, "debug-entity-damage")
            && !configUtils.debugEntityDamageWasEnabled) {
            configUtils.debugEntityDamageWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(entityDamageDebugListener, this);
        } else if (!helperSettings.getBoolean(settingsCfg, "debug-entity-damage")
            && configUtils.debugEntityDamageWasEnabled) {
            configUtils.debugEntityDamageWasEnabled = false;
            HandlerList.unregisterAll(entityDamageDebugListener);
        }

        if (helperSettings.getBoolean(settingsCfg, "ensure-mobs-are-levelled-on-chunk-load")
            && !configUtils.chunkLoadListenerWasEnabled) {
            configUtils.chunkLoadListenerWasEnabled = true;
            Bukkit.getPluginManager().registerEvents(chunkLoadListener, this);
        } else if (!helperSettings.getBoolean(settingsCfg, "ensure-mobs-are-levelled-on-chunk-load")
            && configUtils.chunkLoadListenerWasEnabled) {
            configUtils.chunkLoadListenerWasEnabled = false;
            HandlerList.unregisterAll(chunkLoadListener);
        }

        levelManager.entitySpawnListener.processMobSpawns = helperSettings.getBoolean(settingsCfg,
            "level-mobs-upon-spawn", true);

        levelManager.clearRandomLevellingCache();
        configUtils.playerLevellingEnabled = rulesManager.isPlayerLevellingEnabled();
        rulesManager.clearTempDisabledRulesCounts();
        definitions.useTranslationComponents = helperSettings.getBoolean(settingsCfg,
                "use-translation-components", true);
        definitions.setUseLegacySerializer(helperSettings.getBoolean(settingsCfg,
                "use-legacy-serializer", true));
        nametagQueueManager.nametagSenderHandler.refresh();

        reloadFinishedMsg.forEach(sender::sendMessage);
    }

    public static LevelledMobs getInstance(){
        return instance;
    }

    public @NotNull Definitions getDefinitions(){
        return this.definitions;
    }

    public @NotNull ServerVersionInfo getVerInfo() { return this.ver; }

    @Override
    public void onDisable() {
        final QuickTimer disableTimer = new QuickTimer();
        disableTimer.start();

        levelManager.stopNametagAutoUpdateTask();
        companion.shutDownAsyncTasks();

        Utils.logger.info("Shut-down complete (took " + disableTimer.getTimer() + "ms)");
    }
}
