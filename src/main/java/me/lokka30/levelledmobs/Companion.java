/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.commands.LevelledMobsCommand;
import me.lokka30.levelledmobs.compatibility.Compat1_16;
import me.lokka30.levelledmobs.compatibility.Compat1_17;
import me.lokka30.levelledmobs.customdrops.CustomDropsHandler;
import me.lokka30.levelledmobs.listeners.*;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.managers.LevelManager;
import me.lokka30.levelledmobs.managers.PlaceholderApiIntegration;
import me.lokka30.levelledmobs.managers.WorldGuardIntegration;
import me.lokka30.levelledmobs.misc.FileLoader;
import me.lokka30.levelledmobs.misc.FileMigrator;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.misc.VersionInfo;
import me.lokka30.levelledmobs.rules.MetricsInfo;
import me.lokka30.microlib.UpdateChecker;
import me.lokka30.microlib.VersionUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.PluginManager;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        this.updateResult = new LinkedList<>();
        buildUniversalGroups();
        this.metricsInfo = new MetricsInfo(main);
        this.spawner_CopyIds = new LinkedList<>();
        this.spawner_InfoIds = new LinkedList<>();
    }

    public HashSet<EntityType> groups_HostileMobs;
    public HashSet<EntityType> groups_AquaticMobs;
    public HashSet<EntityType> groups_PassiveMobs;
    public HashSet<EntityType> groups_NetherMobs;
    public List<String> updateResult;
    final public List<UUID> spawner_CopyIds;
    final public List<UUID> spawner_InfoIds;
    public boolean playerInteractListenerIsRegistered;
    final private PluginManager pluginManager = Bukkit.getPluginManager();
    final private MetricsInfo metricsInfo;

    void checkWorldGuard() {
        // Hook into WorldGuard
        // This cannot be moved to onEnable (stated in WorldGuard's documentation). It MUST be ran in onLoad.
        if (ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            main.worldGuardIntegration = new WorldGuardIntegration();
        }
    }

    //Checks if the server version is supported
    public void checkCompatibility() {
        Utils.logger.info("&fCompatibility Checker: &7Checking compatibility with your server...");

        // Using a List system in case more compatibility checks are added.
        final List<String> incompatibilities = new LinkedList<>();

        // Check the MC version of the server.
        if (!VersionUtils.isOneFourteen()) {
            incompatibilities.add("Your server version &8(&b" + Bukkit.getVersion() + "&8)&7 is unsupported by &bLevelledMobs v" + main.getDescription().getVersion() + "&7!" +
                    "Compatible MC versions: &b" + String.join("&7,&b ", Utils.getSupportedServerVersions()) + "&7.");
        }

        if (!ExternalCompatibilityManager.hasProtocolLibInstalled()) {
            incompatibilities.add("Your server does not have &bProtocolLib&7 installed! This means that no levelled nametags will appear on the mobs. If you wish to see custom nametags above levelled mobs, then you must install ProtocolLib.");
        }

        main.incompatibilitiesAmount = incompatibilities.size();
        if (incompatibilities.isEmpty())
            Utils.logger.info("&fCompatibility Checker: &7No incompatibilities found.");
        else {
            Utils.logger.warning("&fCompatibility Checker: &7Found the following possible incompatibilities:");
            incompatibilities.forEach(incompatibility -> Utils.logger.info("&8 - &7" + incompatibility));
        }
    }

    private int getSettingsVersion(){
        final File file = new File(main.getDataFolder(), "settings.yml");
        if (!file.exists()) return 0;

        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return main.helperSettings.getInt(cfg,"file-version");
    }

    // Note: also called by the reload subcommand.
    public boolean loadFiles(final boolean isReload) {
        Utils.logger.info("&fFile Loader: &7Loading files...");

        // save license.txt
        FileLoader.saveResourceIfNotExists(main, new File(main.getDataFolder(), "license.txt"));

        main.rulesParsingManager.parseRulesMain(FileLoader.loadFile(main, "rules", FileLoader.RULES_FILE_VERSION));

        main.configUtils.playerLevellingEnabled = main.rulesManager.isPlayerLevellingEnabled();

        final int settingsVersion = getSettingsVersion();
        if (settingsVersion > 20 && settingsVersion < 30) { // anything older than 2.0 will not be migrated
            FileMigrator.migrateSettingsToRules(main);
        }

        main.settingsCfg = FileLoader.loadFile(main, "settings", FileLoader.SETTINGS_FILE_VERSION);

        if (main.settingsCfg != null) // only load if settings were loaded successfully
            main.messagesCfg = FileLoader.loadFile(main, "messages", FileLoader.MESSAGES_FILE_VERSION);
        else {
            // had an issue reading the file.  Disable the plugin now
            return false;
        }

        main.customDropsHandler = new CustomDropsHandler(main);
        main.customDropsHandler.customDropsParser.loadDrops(
                FileLoader.loadFile(main, "customdrops", FileLoader.CUSTOMDROPS_FILE_VERSION)
        );

        if (!isReload) {
            main.attributesCfg = loadEmbeddedResource("defaultAttributes.yml");
            main.dropsCfg = loadEmbeddedResource("defaultDrops.yml");
            main.mobHeadManager.loadTextures(Objects.requireNonNull(loadEmbeddedResource("textures.yml")));

            // remove legacy files if they exist
            final String[] legacyFile = {"attributes.yml", "drops.yml"};
            for (String lFile : legacyFile) {
                final File delFile = new File(main.getDataFolder(), lFile);
                try {
                    if (delFile.exists()) delFile.delete();
                } catch (Exception e) {
                    Utils.logger.warning("Unable to delete file " + lFile + ", " + e.getMessage());
                }
            }

        }

        final List<String> debugsEnabled = main.settingsCfg.getStringList(main.helperSettings.getKeyNameFromConfig(main.settingsCfg, "debug-misc"));
        if (!debugsEnabled.isEmpty())
            Utils.logger.info("misc debugs enabled: &b" + debugsEnabled);

        main.configUtils.load();
        main.playerLevellingMinRelevelTime = main.helperSettings.getInt(main.settingsCfg, "player-levelling-relevel-min-time", 5000);

        return true;
    }

    @Nullable
    YamlConfiguration loadEmbeddedResource(final String filename) {
        YamlConfiguration result = null;
        final InputStream inputStream = main.getResource(filename);
        if (inputStream == null) return null;

        try {
            InputStreamReader reader = new InputStreamReader(inputStream);
            result = YamlConfiguration.loadConfiguration(reader);
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            Utils.logger.error("Error reading embedded file: " + filename + ", " + e.getMessage());
        }

        return result;
    }

    void registerListeners() {
        Utils.logger.info("&fListeners: &7Registering event listeners...");

        main.levelManager = new LevelManager(main);
        main._mobsQueueManager.start();
        main.nametagQueueManager_.start();
        main.levelManager.entitySpawnListener = new EntitySpawnListener(main); // we're saving this reference so the summon command has access to it
        main.levelManager.entitySpawnListener.processMobSpawns = main.helperSettings.getBoolean(main.settingsCfg, "level-mobs-upon-spawn", true);
        main.entityDamageDebugListener = new EntityDamageDebugListener(main);
        main.blockPlaceListener = new BlockPlaceListener(main);

        if (main.helperSettings.getBoolean(main.settingsCfg, "debug-entity-damage")) {
            // we'll load and unload this listener based on the above setting when reloading
            main.configUtils.debugEntityDamageWasEnabled = true;
            pluginManager.registerEvents(main.entityDamageDebugListener, main);
        }

        pluginManager.registerEvents(main.levelManager.entitySpawnListener, main);
        pluginManager.registerEvents(new EntityDamageListener(main), main);
        pluginManager.registerEvents(new EntityDeathListener(main), main);
        pluginManager.registerEvents(new EntityRegainHealthListener(main), main);
        pluginManager.registerEvents(new EntityTransformListener(main), main);
        pluginManager.registerEvents(new EntityNametagListener(main), main);
        pluginManager.registerEvents(new EntityTargetListener(main), main);
        pluginManager.registerEvents(new PlayerJoinListener(main), main);
        pluginManager.registerEvents(new EntityTameListener(main), main);
        pluginManager.registerEvents(new PlayerDeathListener(main), main);
        pluginManager.registerEvents(new CombustListener(main), main);
        pluginManager.registerEvents(main.blockPlaceListener, main);
        main.chunkLoadListener = new ChunkLoadListener(main);
        main.playerInteractEventListener = new PlayerInteractEventListener(main);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            main.placeholderApiIntegration = new PlaceholderApiIntegration(main);
            main.placeholderApiIntegration.register();
        }

        if (main.helperSettings.getBoolean(main.settingsCfg,"ensure-mobs-are-levelled-on-chunk-load", true))
            pluginManager.registerEvents(main.chunkLoadListener, main);
    }

    void registerCommands() {
        Utils.logger.info("&fCommands: &7Registering commands...");

        final PluginCommand levelledMobsCommand = main.getCommand("levelledmobs");
        if (levelledMobsCommand == null) {
            Utils.logger.error("Command &b/levelledmobs&7 is unavailable, is it not registered in plugin.yml?");
        } else {
            levelledMobsCommand.setExecutor(new LevelledMobsCommand(main));
        }
    }

    void loadSpigotConfig(){
        try {
            main.levelManager.attributeMaxHealthMax = Bukkit.getServer().spigot().getConfig().getDouble("settings.attribute.maxHealth.max", 2048.0);
            main.levelManager.attributeMovementSpeedMax = Bukkit.getServer().spigot().getConfig().getDouble("settings.attribute.movementSpeed.max", 2048.0);
            main.levelManager.attributeAttackDamageMax = Bukkit.getServer().spigot().getConfig().getDouble("settings.attribute.attackDamage.max", 2048.0);
        } catch (NoSuchMethodError ignored) {
            main.levelManager.attributeMaxHealthMax = Integer.MAX_VALUE;
            main.levelManager.attributeMovementSpeedMax = Integer.MAX_VALUE;
            main.levelManager.attributeAttackDamageMax = Integer.MAX_VALUE;
        }
    }

    void setupMetrics() {
        final Metrics metrics = new Metrics(main, 6269);

        metrics.addCustomChart(new SimplePie("maxlevel_used", metricsInfo::getMaxLevelRange));
        metrics.addCustomChart(new SimplePie("custom_rules_used", metricsInfo::getCustomRulesUsed));
        metrics.addCustomChart(new SimplePie("custom_drops_enabled", metricsInfo::getUsesCustomDrops));
        metrics.addCustomChart(new SimplePie("health_indicator_enabled", metricsInfo::getUsesHealthIndicator));
        metrics.addCustomChart(new SimplePie("levelling_strategy", metricsInfo::getLevellingStrategy));
        metrics.addCustomChart(new SimplePie("autoupdate_checker_enabled", metricsInfo::usesAutoUpdateChecker));
        metrics.addCustomChart(new SimplePie("level_mobs_upon_spawn", metricsInfo::levelMobsUponSpawn));
        metrics.addCustomChart(new SimplePie("check_mobs_on_chunk_load", metricsInfo::checkMobsOnChunkLoad));
        metrics.addCustomChart(new SimplePie("custom-entity-names", metricsInfo::customEntityNamesCount));
        metrics.addCustomChart(new SimplePie("utilizes-nbtdata", metricsInfo::usesNbtData));
        metrics.addCustomChart(new SimplePie("utilizes_player_levelling", metricsInfo::usesPlayerLevelling));
        metrics.addCustomChart(new SimplePie("nametag_visibility", metricsInfo::nametagVisibility));
        metrics.addCustomChart(new SimpleBarChart("enabled-compatibility", metricsInfo::enabledCompats));
    }

    //Check for updates on the Spigot page.
    void checkUpdates() {
        if (main.helperSettings.getBoolean(main.settingsCfg,"use-update-checker", true)) {
            final UpdateChecker updateChecker = new UpdateChecker(main, 74304);
            updateChecker.getLatestVersion(latestVersion -> {
                final String currentVersion = updateChecker.getCurrentVersion().split(" ")[0];

                VersionInfo thisVersion;
                VersionInfo spigotVersion;
                boolean isOutOfDate;
                boolean isNewerVersion;

                try {
                    thisVersion = new VersionInfo(currentVersion);
                    spigotVersion = new VersionInfo(latestVersion);

                    isOutOfDate = (thisVersion.compareTo(spigotVersion) < 0);
                    isNewerVersion = (thisVersion.compareTo(spigotVersion) > 0);
                } catch (InvalidObjectException e) {
                    Utils.logger.warning("Got exception creating version objects: " + e.getMessage());

                    isOutOfDate = !currentVersion.equals(latestVersion);
                    isNewerVersion = currentVersion.contains("indev");
                }

                if (isNewerVersion) {
                    updateResult = Collections.singletonList(
                            "&7Your &bLevelledMobs&7 version is &ba pre-release&7. Latest release version is &bv%latestVersion%&7. &8(&7You're running &bv%currentVersion%&8)");

                    updateResult = Utils.replaceAllInList(updateResult, "%currentVersion%", currentVersion);
                    updateResult = Utils.replaceAllInList(updateResult, "%latestVersion%", latestVersion);
                    updateResult = Utils.colorizeAllInList(updateResult);

                    updateResult.forEach(Utils.logger::warning);
                } else if (isOutOfDate) {

                    // for some reason config#getStringList doesn't allow defaults??
                    if (main.messagesCfg.contains("other.update-notice.messages")) {
                        updateResult = main.messagesCfg.getStringList("other.update-notice.messages");
                    } else {
                        updateResult = Arrays.asList(
                                "&b&nLevelledMobs Update Checker Notice:",
                                "&7Your &bLevelledMobs&7 version is &boutdated&7! Please update to" +
                                        "&bv%latestVersion%&7 as soon as possible. &8(&7You''re running &bv%currentVersion%&8)");
                    }

                    updateResult = Utils.replaceAllInList(updateResult, "%currentVersion%", currentVersion);
                    updateResult = Utils.replaceAllInList(updateResult, "%latestVersion%", latestVersion);
                    updateResult = Utils.colorizeAllInList(updateResult);

                    if (main.messagesCfg.getBoolean("other.update-notice.send-in-console", true))
                        updateResult.forEach(Utils.logger::warning);

                    // notify any players that may be online already
                    if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true)) {
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
                            if (onlinePlayer.hasPermission("levelledmobs.receive-update-notifications")) {
                                for (String msg : updateResult) {
                                    onlinePlayer.sendMessage(msg);
                                }
                                //updateResult.forEach(onlinePlayer::sendMessage); //compiler didn't like this :(
                            }
                        });
                    }
                }
            });
        }
    }

    void shutDownAsyncTasks() {
        Utils.logger.info("&fTasks: &7Shutting down other async tasks...");
        main._mobsQueueManager.stop();
        main.nametagQueueManager_.stop();
        Bukkit.getScheduler().cancelTasks(main);
    }

    private void buildUniversalGroups(){

        // include interfaces: Monster, Boss
        groups_HostileMobs = Stream.of(
                EntityType.ENDER_DRAGON,
                EntityType.GHAST,
                EntityType.MAGMA_CUBE,
                EntityType.PHANTOM,
                EntityType.SHULKER,
                EntityType.SLIME
        ).collect(Collectors.toCollection(HashSet::new));

        if (VersionUtils.isOneSeventeen() || VersionUtils.isOneSixteen())
            groups_HostileMobs.addAll(Compat1_16.getHostileMobs());

        // include interfaces: Animals, WaterMob
        groups_PassiveMobs = Stream.of(
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN
        ).collect(Collectors.toCollection(HashSet::new));

        if (VersionUtils.isOneSeventeen())
            groups_PassiveMobs.addAll(Compat1_17.getPassiveMobs());

        if (VersionUtils.isOneSeventeen() || VersionUtils.isOneSixteen())
            groups_HostileMobs.addAll(Compat1_16.getPassiveMobs());

        // include interfaces: WaterMob
        groups_AquaticMobs = Stream.of(
                EntityType.DROWNED,
                EntityType.ELDER_GUARDIAN,
                EntityType.GUARDIAN,
                EntityType.TURTLE
        ).collect(Collectors.toCollection(HashSet::new));
    }
}
