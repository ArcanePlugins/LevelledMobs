/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.customdrops.CustomDropHandler;
import me.lokka30.levelledmobs.debug.DebugHandler;
import me.lokka30.levelledmobs.file.FileHandler;
import me.lokka30.levelledmobs.integration.IntegrationHandler;
import me.lokka30.levelledmobs.level.LevelHandler;
import me.lokka30.levelledmobs.listener.*;
import me.lokka30.levelledmobs.metric.MetricsHandler;
import me.lokka30.levelledmobs.nametag.NametagHandler;
import me.lokka30.levelledmobs.nms.NMSHandler;
import me.lokka30.levelledmobs.queue.QueueHandler;
import me.lokka30.levelledmobs.translations.TranslationHandler;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.microlib.maths.QuickTimer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author lokka30
 * @since v4.0.0
 * Main class of the plugin. Acts as a 'hub' of sorts in the plugin's code.
 */
public final class LevelledMobs extends JavaPlugin {

    private static LevelledMobs instance;
    public static LevelledMobs getInstance() { return instance; }

    private final QueueHandler queueHandler = new QueueHandler();
    public @NotNull QueueHandler getQueueHandler() { return queueHandler; }

    private final TranslationHandler translationHandler = new TranslationHandler();
    public @NotNull TranslationHandler getTranslationHandler() { return translationHandler; }

    private final NametagHandler nametagHandler = new NametagHandler();
    public @NotNull NametagHandler getNametagHandler() { return nametagHandler; }

    private final MetricsHandler metricsHandler = new MetricsHandler();
    public @NotNull MetricsHandler getMetricsHandler() { return metricsHandler; }

    private final LevelHandler levelHandler = new LevelHandler();
    public @NotNull LevelHandler getLevelHandler() { return levelHandler; }

    private final IntegrationHandler integrationHandler = new IntegrationHandler();
    public @NotNull IntegrationHandler getIntegrationHandler() { return integrationHandler; }

    private final DebugHandler debugHandler = new DebugHandler();
    public @NotNull DebugHandler getDebugHandler() { return debugHandler; }

    private final CustomDropHandler customDropHandler = new CustomDropHandler();
    public @NotNull CustomDropHandler getCustomDropHandler() { return customDropHandler; }

    private final FileHandler fileHandler = new FileHandler();
    public @NotNull FileHandler getFileHandler() { return fileHandler; }

    private final NMSHandler nmsHandler = new NMSHandler();
    public @NotNull NMSHandler getNMSHandler() { return nmsHandler; }

    /* Start-up & shut-down methods */

    /**
     * @author lokka30
     * @since v4.0.0
     * Called by Bukkit's plugin manager
     * in the 'loading' stage of the server.
     * This runs before 'onEnable', so any
     * important things to get done before
     * 'onEnable' must be added here.
     */
    @Override
    public void onLoad() {
        instance = this;

        final QuickTimer timer = new QuickTimer();
        Utils.LOGGER.info("&f~ Beginning initialization sequence ~");

        //TODO lokka30: Complete this method's body.

        Utils.LOGGER.info("&f~ Initialization sequence complete, took &b" + timer.getTimer() + "ms&f ~");
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * Called by Bukkit's plugin manager
     * when it enables the plugin.
     * Ensure reloads are factored in to
     * any code ran inside this method.
     * Warning: Methods are ordered on purpose,
     * as some code requires other code to be
     * ran first (e.g. listeners require configs
     * to be loaded first).
     */
    @Override
    public void onEnable() {
        final QuickTimer timer = new QuickTimer();
        Utils.LOGGER.info("&f~ Beginning start-up sequence ~");

        // IMPORTANT: Do not mess with the order of these methods being ran!

        //TODO lokka30: Complete this method's body.
        fileHandler.loadAll(false);

        debugHandler.load();

        queueHandler.startQueues();

        loadListeners();

        Utils.LOGGER.info("&f~ Start-up sequence complete, took &b" + timer.getTimer() + "ms&f ~");
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * Called by Bukkit's plugin manager
     * when it enables the plugin.
     * Ensure reloads are factored in to
     * any code ran inside this method.
     */
    @Override
    public void onDisable() {
        final QuickTimer timer = new QuickTimer();
        Utils.LOGGER.info("&f~ Beginning shut-down sequence ~");

        // IMPORTANT: Do not mess with the order of these methods being ran!

        //TODO lokka30: Complete this method's body.

        queueHandler.stopQueues();

        Utils.LOGGER.info("&f~ Shut-down sequence complete, took &b" + timer.getTimer() + "ms&f ~");
    }

    /* Methods ran by onLoad, onEnable and onDisable */

    /**
     * @author lokka30
     * @since v4.0.0
     * Registers ALL of LevelledMobs' listener classes through Bukkit's plugin manager.
     * Only to be ran from onEnable, do not use elsewhere.
     * The HashSet of Listeners must be updated manually if a new Listener is added to LM.
     * @see me.lokka30.levelledmobs.listener
     * @see org.bukkit.plugin.PluginManager#registerEvents(Listener, Plugin)
     */
    private void loadListeners() {
        Utils.LOGGER.info("Loading listeners...");

        // Retain alphabetical order when modifying this list! :)
        Set.of(
                new BlockPlaceListener(),
                new ChunkLoadListener(),
                new EntityCombustListener(),
                new EntityDamageByEntityListener(),
                new EntityDamageListener(),
                new EntityDeathListener(),
                new EntityRegainHealthListener(),
                new EntitySpawnListener(),
                new EntityTameListener(),
                new EntityTargetListener(),
                new EntityTransformListener(),
                new PlayerChangedWorldListener(),
                new PlayerDeathListener(),
                new PlayerInteractEntityListener(),
                new PlayerInteractListener(),
                new PlayerJoinListener(),
                new PlayerQuitListener(),
                new PlayerTeleportListener()
        ).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }
}
