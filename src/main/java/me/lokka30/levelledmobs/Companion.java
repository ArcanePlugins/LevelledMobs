/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.lokka30.levelledmobs.commands.LevelledMobsCommand;
import me.lokka30.levelledmobs.compatibility.Compat1_16;
import me.lokka30.levelledmobs.compatibility.Compat1_17;
import me.lokka30.levelledmobs.compatibility.Compat1_19;
import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.BlockPlaceListener;
import me.lokka30.levelledmobs.listeners.ChunkLoadListener;
import me.lokka30.levelledmobs.listeners.CombustListener;
import me.lokka30.levelledmobs.listeners.EntityDamageDebugListener;
import me.lokka30.levelledmobs.listeners.EntityDamageListener;
import me.lokka30.levelledmobs.listeners.EntityNametagListener;
import me.lokka30.levelledmobs.listeners.EntityPickupItemListener;
import me.lokka30.levelledmobs.listeners.EntityRegainHealthListener;
import me.lokka30.levelledmobs.listeners.EntitySpawnListener;
import me.lokka30.levelledmobs.listeners.EntityTameListener;
import me.lokka30.levelledmobs.listeners.EntityTargetListener;
import me.lokka30.levelledmobs.listeners.EntityTransformListener;
import me.lokka30.levelledmobs.listeners.PlayerDeathListener;
import me.lokka30.levelledmobs.listeners.PlayerInteractEventListener;
import me.lokka30.levelledmobs.listeners.PlayerJoinListener;
import me.lokka30.levelledmobs.listeners.PlayerPortalEventListener;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.PlaceholderApiIntegration;
import me.lokka30.levelledmobs.misc.ChunkKillInfo;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.FileLoader;
import me.lokka30.levelledmobs.misc.FileMigrator;
import me.lokka30.levelledmobs.misc.OutdatedServerVersionException;
import me.lokka30.levelledmobs.misc.VersionInfo;
import me.lokka30.levelledmobs.nametag.ServerVersionInfo;
import me.lokka30.levelledmobs.rules.MetricsInfo;
import me.lokka30.levelledmobs.util.UpdateChecker;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.SchedulerResult;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class contains methods used by the main class.
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
public class Companion {

    private final LevelledMobs main;

    Companion(final LevelledMobs main) {
        this.main = main;
        this.recentlyJoinedPlayers = new WeakHashMap<>();
        this.playerNetherPortals = new HashMap<>();
        this.playerWorldPortals = new HashMap<>();
        this.updateResult = new LinkedList<>();
        buildUniversalGroups();
        this.metricsInfo = new MetricsInfo(main);
        this.spawnerCopyIds = new LinkedList<>();
        this.spawnerInfoIds = new LinkedList<>();
        this.entityDeathInChunkCounter = new HashMap<>();
        this.chunkKillNoticationTracker = new HashMap<>();
        this.externalCompatibilityManager = new ExternalCompatibilityManager();
    }

    final private WeakHashMap<Player, Instant> recentlyJoinedPlayers;
    public HashSet<EntityType> hostileMobsGroup;
    public HashSet<EntityType> aquaticMobsGroup;
    public HashSet<EntityType> passiveMobsGroup;
    public List<String> updateResult;
    private boolean hadRulesLoadError;
    public boolean useAdventure;
    public CommandSender reloadSender;
    final private HashMap<Long, Map<EntityType, ChunkKillInfo>> entityDeathInChunkCounter;
    final private HashMap<Long, Map<UUID, Instant>> chunkKillNoticationTracker;
    final public Map<Player, Location> playerNetherPortals;
    final public Map<Player, Location> playerWorldPortals;
    final public List<UUID> spawnerCopyIds;
    final public List<UUID> spawnerInfoIds;
    final private PluginManager pluginManager = Bukkit.getPluginManager();
    final private MetricsInfo metricsInfo;
    final public ExternalCompatibilityManager externalCompatibilityManager;
    private SchedulerResult hashMapCleanUp;
    final static private Object playerLogonTimesLock = new Object();
    final static private Object playerNetherPortalsLock = new Object();
    final static private Object entityDeathInChunkCounterLock = new Object();
    final static private Object entityDeathInChunkNotifierLock = new Object();

    public boolean getHadRulesLoadError() {
        return this.hadRulesLoadError;
    }

    private int getSettingsVersion() {
        final File file = new File(main.getDataFolder(), "settings.yml");
        if (!file.exists()) {
            return 0;
        }

        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return main.helperSettings.getInt(cfg, "file-version");
    }

    // Note: also called by the reload subcommand.
    boolean loadFiles(final boolean isReload) {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        final YamlConfiguration rulesFile = FileLoader.loadFile(main, "rules",
            FileLoader.RULES_FILE_VERSION);
        this.hadRulesLoadError = rulesFile == null;
        main.rulesParsingManager.parseRulesMain(rulesFile);

        main.configUtils.playerLevellingEnabled = main.rulesManager.isPlayerLevellingEnabled();

        final int settingsVersion = getSettingsVersion();
        if (settingsVersion > 20
            && settingsVersion < 30) { // anything older than 2.0 will not be migrated
            FileMigrator.migrateSettingsToRules(main);
        }

        main.settingsCfg = FileLoader.loadFile(main, "settings", FileLoader.SETTINGS_FILE_VERSION);

        if (main.settingsCfg != null) // only load if settings were loaded successfully
        {
            main.messagesCfg = FileLoader.loadFile(main, "messages",
                FileLoader.MESSAGES_FILE_VERSION);
        } else {
            // had an issue reading the file.  Disable the plugin now
            return false;
        }

        main.customDropsHandler = new CustomDropsHandler(main);

        if (!isReload) {
            main.dropsCfg = loadEmbeddedResource("defaultDrops.yml");

            // remove legacy files if they exist
            final String[] legacyFile = {"attributes.yml", "drops.yml"};
            for (final String lFile : legacyFile) {
                final File delFile = new File(main.getDataFolder(), lFile);
                try {
                    if (delFile.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        delFile.delete();
                    }
                } catch (final Exception e) {
                    Utils.logger.warning("Unable to delete file " + lFile + ", " + e.getMessage());
                }
            }

            parseDebugsEnabled();
        } else {
            // if not reloading then it is called from the server load event to make sure any dependent
            // plugins are already loaded
            parseDebugsEnabled();
            main.customDropsHandler.customDropsParser.loadDrops(
                FileLoader.loadFile(main, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
            );
        }

        main.configUtils.load();
        main.playerLevellingMinRelevelTime = main.helperSettings.getIntTimeUnitMS(main.settingsCfg,
            "player-levelling-relevel-min-time", 5000L);
        this.useAdventure = main.helperSettings.getBoolean(main.settingsCfg, "use-adventure", true);

        return true;
    }

    private void parseDebugsEnabled() {
        final List<String> debugsEnabled = main.settingsCfg.getStringList(
            main.helperSettings.getKeyNameFromConfig(main.settingsCfg, "debug-misc"));
        if (debugsEnabled.isEmpty()) {
            return;
        }

        boolean useAllDebugs = false;
        boolean addedDebugs = false;
        for (final String debug : debugsEnabled) {
            if (Utils.isNullOrEmpty(debug)) {
                continue;
            }

            if ("*".equalsIgnoreCase(debug)){
                useAllDebugs = true;
                continue;
            }

            try {
                final DebugType debugType = DebugType.valueOf(debug.toUpperCase());
                main.debugManager.filterDebugTypes.add(debugType);
                addedDebugs = true;
            } catch (final Exception ignored) {
                Utils.logger.warning("Invalid value for debug-misc: " + debug);
            }
        }

        if (useAllDebugs){
            main.debugManager.filterDebugTypes.clear();
        }

        if (addedDebugs && !main.debugManager.isEnabled()) {
            final CommandSender useSender = this.reloadSender != null ?
                    this.reloadSender : Bukkit.getConsoleSender();
            main.debugManager.enableDebug(useSender);
            useSender.sendMessage(main.debugManager.getDebugStatus());
        }
    }

    @Nullable private YamlConfiguration loadEmbeddedResource(final String filename) {
        YamlConfiguration result = null;
        final InputStream inputStream = main.getResource(filename);
        if (inputStream == null) {
            return null;
        }

        try {
            final InputStreamReader reader = new InputStreamReader(inputStream);
            result = YamlConfiguration.loadConfiguration(reader);
            reader.close();
            inputStream.close();
        } catch (final IOException e) {
            Utils.logger.error("Error reading embedded file: " + filename + ", " + e.getMessage());
        }

        return result;
    }

    void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        main.levelManager = new LevelManager(main);
        main.mobsQueueManager.start();
        main.nametagQueueManager.start();
        main.levelManager.entitySpawnListener = new EntitySpawnListener(main);
        main.levelManager.entitySpawnListener.processMobSpawns = main.helperSettings.getBoolean(
            main.settingsCfg, "level-mobs-upon-spawn", true);
        main.entityDamageDebugListener = new EntityDamageDebugListener(main);
        main.blockPlaceListener = new BlockPlaceListener(main);

        if (main.helperSettings.getBoolean(main.settingsCfg, "debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            main.configUtils.debugEntityDamageWasEnabled = true;
            pluginManager.registerEvents(main.entityDamageDebugListener, main);
        }

        pluginManager.registerEvents(main.levelManager.entitySpawnListener, main);
        pluginManager.registerEvents(new EntityDamageListener(main), main);
        pluginManager.registerEvents(main.entityDeathListener, main);
        pluginManager.registerEvents(new EntityRegainHealthListener(main), main);
        pluginManager.registerEvents(new EntityTransformListener(main), main);
        pluginManager.registerEvents(new EntityNametagListener(main), main);
        pluginManager.registerEvents(new EntityTargetListener(main), main);
        pluginManager.registerEvents(new PlayerJoinListener(main), main);
        pluginManager.registerEvents(new EntityTameListener(main), main);
        pluginManager.registerEvents(new PlayerDeathListener(main), main);
        pluginManager.registerEvents(new CombustListener(main), main);
        pluginManager.registerEvents(main.blockPlaceListener, main);
        pluginManager.registerEvents(new PlayerPortalEventListener(main), main);
        pluginManager.registerEvents(new EntityPickupItemListener(), main);
        main.chunkLoadListener = new ChunkLoadListener(main);
        main.playerInteractEventListener = new PlayerInteractEventListener(main);
        pluginManager.registerEvents(main.playerInteractEventListener, main);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            main.placeholderApiIntegration = new PlaceholderApiIntegration(main);
            main.placeholderApiIntegration.register();
        }

        if (main.helperSettings.getBoolean(main.settingsCfg,
            "ensure-mobs-are-levelled-on-chunk-load", true)) {
            pluginManager.registerEvents(main.chunkLoadListener, main);
        }
    }

    void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        main.levelledMobsCommand = new LevelledMobsCommand(main);
        final PluginCommand levelledMobsCommand = main.getCommand("levelledmobs");
        if (levelledMobsCommand == null) {
            Utils.logger.error(
                "Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?");
        } else {
            levelledMobsCommand.setExecutor(main.levelledMobsCommand);
        }
    }

    void setupMetrics() {
        final Metrics metrics = new Metrics(main, 6269);

        metrics.addCustomChart(new SimplePie("maxlevel_used", metricsInfo::getMaxLevelRange));
        metrics.addCustomChart(new SimplePie("custom_rules_used", metricsInfo::getCustomRulesUsed));
        metrics.addCustomChart(
            new SimplePie("custom_drops_enabled", metricsInfo::getUsesCustomDrops));
        metrics.addCustomChart(
            new SimplePie("health_indicator_enabled", metricsInfo::getUsesHealthIndicator));
        metrics.addCustomChart(
            new SimplePie("levelling_strategy", metricsInfo::getLevellingStrategy));
        metrics.addCustomChart(
            new SimplePie("autoupdate_checker_enabled", metricsInfo::usesAutoUpdateChecker));
        metrics.addCustomChart(
            new SimplePie("level_mobs_upon_spawn", metricsInfo::levelMobsUponSpawn));
        metrics.addCustomChart(
            new SimplePie("check_mobs_on_chunk_load", metricsInfo::checkMobsOnChunkLoad));
        metrics.addCustomChart(
            new SimplePie("custom-entity-names", metricsInfo::customEntityNamesCount));
        metrics.addCustomChart(new SimplePie("utilizes-nbtdata", metricsInfo::usesNbtData));
        metrics.addCustomChart(
            new SimplePie("utilizes_player_levelling", metricsInfo::usesPlayerLevelling));
        metrics.addCustomChart(new SimplePie("nametag_visibility", metricsInfo::nametagVisibility));
        metrics.addCustomChart(
            new SimpleBarChart("enabled-compatibility", metricsInfo::enabledCompats));
    }

    void startCleanupTask() {
        final SchedulerWrapper scheduler = new SchedulerWrapper(() -> {
            synchronized (entityDeathInChunkCounterLock) {
                chunkKillLimitCleanup();
            }
            synchronized (entityDeathInChunkNotifierLock) {
                chunkKillNoticationCleanup();
            }
        });

        this.hashMapCleanUp = scheduler.runTaskTimerAsynchronously(5000, 2000);
    }

    private void chunkKillLimitCleanup() {
        final List<Long> chunkKeysToRemove = new LinkedList<>();

        for (final long chunkKey : entityDeathInChunkCounter.keySet()) {
            //                                 Cooldown time, entity counts
            final Map<EntityType, ChunkKillInfo> pairList = entityDeathInChunkCounter.get(chunkKey);

            if (pairList == null) {
                continue;
            }
            final Instant now = Instant.now();

            for (final EntityType entityType : pairList.keySet()) {
                final ChunkKillInfo chunkKillInfo = pairList.get(entityType);

                chunkKillInfo.getEntrySet().removeIf(
                    e -> e.getKey().compareTo(now.minusSeconds(e.getValue())) < 0
                );
            }

            pairList.entrySet().removeIf(e -> e.getValue().isEmpty());

            if (pairList.isEmpty()) {
                // Remove the object to prevent iterate over exceed amount of empty pairList
                chunkKeysToRemove.add(chunkKey);
            }
        }

        for (final long chunkKey : chunkKeysToRemove) {
            entityDeathInChunkCounter.remove(chunkKey);
        }
    }

    private void chunkKillNoticationCleanup() {
        final Iterator<Long> iterator = this.chunkKillNoticationTracker.keySet().iterator();

        while (iterator.hasNext()) {
            final long chunkKey = iterator.next();
            final Map<UUID, Instant> playerTimestamps = this.chunkKillNoticationTracker.get(
                chunkKey);
            playerTimestamps.entrySet()
                .removeIf(e -> Duration.between(e.getValue(), Instant.now()).toSeconds() > 30L);

            if (playerTimestamps.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @NotNull public Map<EntityType, ChunkKillInfo> getorAddPairForSpecifiedChunk(final long chunkKey) {
        synchronized (entityDeathInChunkCounterLock) {
            return this.entityDeathInChunkCounter.computeIfAbsent(chunkKey, k -> new HashMap<>());
        }
    }

    @NotNull public List<Map<EntityType, ChunkKillInfo>> getorAddPairForSpecifiedChunks(
        final @NotNull List<Long> chunkKeys) {
        final List<Map<EntityType, ChunkKillInfo>> results = new ArrayList<>(chunkKeys.size());

        synchronized (entityDeathInChunkCounterLock) {
            for (final long chunkKey : chunkKeys) {
                results.add(
                    this.entityDeathInChunkCounter.computeIfAbsent(chunkKey, k -> new HashMap<>()));
            }
        }

        return results;
    }

    public boolean doesUserHaveCooldown(final @NotNull List<Long> chunkKeys,
        final @NotNull UUID userId) {
        final List<Map<UUID, Instant>> chunkInfos = new LinkedList<>();

        synchronized (entityDeathInChunkNotifierLock) {
            for (final long chunkKey : chunkKeys) {
                if (this.chunkKillNoticationTracker.containsKey(chunkKey)) {
                    chunkInfos.add(this.chunkKillNoticationTracker.get(chunkKey));
                }
            }
        }

        if (chunkInfos.isEmpty()) {
            return false;
        }

        for (final Map<UUID, Instant> chunkInfo : chunkInfos) {
            if (chunkInfo == null || !chunkInfo.containsKey(userId)) {
                continue;
            }
            final Instant instant = chunkInfo.get(userId);
            if (Duration.between(instant, Instant.now()).toSeconds() <= 30L) {
                return true;
            }
        }

        return false;
    }

    public void addUserCooldown(final @NotNull List<Long> chunkKeys, final @NotNull UUID userId) {
        synchronized (entityDeathInChunkNotifierLock) {
            for (final long chunkKey : chunkKeys) {
                final Map<UUID, Instant> entry = this.chunkKillNoticationTracker.computeIfAbsent(
                    chunkKey, k -> new HashMap<>());
                entry.put(userId, Instant.now());
            }
        }
    }

    public void clearChunkKillCache() {
        synchronized (entityDeathInChunkCounterLock) {
            this.entityDeathInChunkCounter.clear();
        }
        synchronized (entityDeathInChunkNotifierLock) {
            this.chunkKillNoticationTracker.clear();
        }
    }

    //Check for updates on the Spigot page.
    void checkUpdates() {
        if (main.helperSettings.getBoolean(main.settingsCfg, "use-update-checker", true)) {
            final UpdateChecker updateChecker = new UpdateChecker(main, 74304);
            try {
                updateChecker.getLatestVersion(latestVersion -> {
                    final String currentVersion = updateChecker.getCurrentVersion().split(" ")[0];

                    final VersionInfo thisVersion;
                    final VersionInfo spigotVersion;
                    boolean isOutOfDate;
                    boolean isNewerVersion;

                    try {
                        thisVersion = new VersionInfo(currentVersion);
                        spigotVersion = new VersionInfo(latestVersion);

                        isOutOfDate = (thisVersion.compareTo(spigotVersion) < 0);
                        isNewerVersion = (thisVersion.compareTo(spigotVersion) > 0);
                    } catch (final InvalidObjectException e) {
                        Utils.logger.warning(
                            "Got exception creating version objects: " + e.getMessage());

                        isOutOfDate = !currentVersion.equals(latestVersion);
                        isNewerVersion = currentVersion.contains("indev");
                    }

                    if (isNewerVersion) {
                        updateResult = List.of(
                            "&7Your &bLevelledMobs&7 version is &ba pre-release&7. Latest release version is &bv%latestVersion%&7. &8(&7You're running &bv%currentVersion%&8)");

                        updateResult = Utils.replaceAllInList(updateResult, "%currentVersion%",
                            currentVersion);
                        updateResult = Utils.replaceAllInList(updateResult, "%latestVersion%",
                            latestVersion);
                        updateResult = Utils.colorizeAllInList(updateResult);

                        updateResult.forEach(Utils.logger::warning);
                    } else if (isOutOfDate) {

                        // for some reason config#getStringList doesn't allow defaults??
                        if (main.messagesCfg.contains("other.update-notice.messages")) {
                            updateResult = main.messagesCfg.getStringList(
                                "other.update-notice.messages");
                        } else {
                            updateResult = List.of(
                                "&b&nLevelledMobs Update Checker Notice:",
                                "&7Your &bLevelledMobs&7 version is &boutdated&7! Please update to"
                                    +
                                    "&bv%latestVersion%&7 as soon as possible. &8(&7You''re running &bv%currentVersion%&8)");
                        }

                        updateResult = Utils.replaceAllInList(updateResult, "%currentVersion%",
                            currentVersion);
                        updateResult = Utils.replaceAllInList(updateResult, "%latestVersion%",
                            latestVersion);
                        updateResult = Utils.colorizeAllInList(updateResult);

                        if (main.messagesCfg.getBoolean("other.update-notice.send-in-console",
                            true)) {
                            updateResult.forEach(Utils.logger::warning);
                        }

                        // notify any players that may be online already
                        if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true)) {
                            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                                if (onlinePlayer.hasPermission(
                                    "levelledmobs.receive-update-notifications")) {
                                    for (final String msg : updateResult) {
                                        onlinePlayer.sendMessage(msg);
                                    }
                                    //updateResult.forEach(onlinePlayer::sendMessage); //compiler didn't like this :(
                                }
                            });
                        }
                    }
                });
            } catch (final OutdatedServerVersionException e) {
                e.printStackTrace();
            }
        }
    }

    void shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...");
        main.mobsQueueManager.stop();
        main.nametagQueueManager.stop();
        if (hashMapCleanUp != null) {
            hashMapCleanUp.cancelTask();
        }
        if (!main.getVerInfo().getIsRunningFolia()) {
            Bukkit.getScheduler().cancelTasks(main);
        }
    }

    private void buildUniversalGroups() {
        final ServerVersionInfo versionInfo = main.nametagQueueManager.nametagSenderHandler.versionInfo;

        // include interfaces: Monster, Boss
        hostileMobsGroup = Stream.of(
            EntityType.ENDER_DRAGON,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.PHANTOM,
            EntityType.SHULKER,
            EntityType.SLIME
        ).collect(Collectors.toCollection(HashSet::new));

        if (versionInfo.getMinecraftVersion() >= 1.16) {
            hostileMobsGroup.addAll(Compat1_16.getHostileMobs());
        }

        // include interfaces: Animals, WaterMob
        passiveMobsGroup = Stream.of(
            EntityType.IRON_GOLEM,
            EntityType.SNOWMAN
        ).collect(Collectors.toCollection(HashSet::new));

        if (versionInfo.getMinecraftVersion() >= 1.17) {
            passiveMobsGroup.addAll(Compat1_17.getPassiveMobs());
        }
        if (versionInfo.getMajorVersion() >= 1.19) {
            passiveMobsGroup.addAll(Compat1_19.getPassiveMobs());
        }

        if (versionInfo.getMajorVersion() >= 1.16) {
            hostileMobsGroup.addAll(Compat1_16.getHostileMobs());
        }
        if (versionInfo.getMajorVersion() >= 1.19) {
            hostileMobsGroup.addAll(Compat1_19.getHostileMobs());
        }

        // include interfaces: WaterMob
        aquaticMobsGroup = Stream.of(
            EntityType.DROWNED,
            EntityType.ELDER_GUARDIAN,
            EntityType.GUARDIAN,
            EntityType.TURTLE
        ).collect(Collectors.toCollection(HashSet::new));

        if (versionInfo.getMajorVersion() >= 1.19) {
            aquaticMobsGroup.addAll(Compat1_19.getAquaticMobs());
        }
    }

    public void addRecentlyJoinedPlayer(final Player player) {
        synchronized (playerLogonTimesLock) {
            recentlyJoinedPlayers.put(player, Instant.now());
        }
    }

    @Nullable public Instant getRecentlyJoinedPlayerLogonTime(final Player player) {
        synchronized (playerLogonTimesLock) {
            return recentlyJoinedPlayers.get(player);
        }
    }

    public void removeRecentlyJoinedPlayer(final Player player) {
        synchronized (playerLogonTimesLock) {
            recentlyJoinedPlayers.remove(player);
        }
    }

    @Nullable public Location getPlayerNetherPortalLocation(final @NotNull Player player) {
        synchronized (playerNetherPortalsLock) {
            return playerNetherPortals.get(player);
        }
    }

    public void setPlayerNetherPortalLocation(final @NotNull Player player,
        final @Nullable Location location) {
        synchronized (playerNetherPortalsLock) {
            playerNetherPortals.put(player, location);
        }
    }

    @Nullable public Location getPlayerWorldPortalLocation(final @NotNull Player player) {
        synchronized (playerNetherPortalsLock) {
            return playerWorldPortals.get(player);
        }
    }

    public void setPlayerWorldPortalLocation(final @NotNull Player player,
        final @Nullable Location location) {
        synchronized (playerNetherPortalsLock) {
            playerWorldPortals.put(player, location);
        }
    }
}
