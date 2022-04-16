/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old;

import java.util.Set;
import java.util.logging.Logger;
import me.lokka30.levelledmobs.plugin.old.customdrop.CustomDropHandler;
import me.lokka30.levelledmobs.plugin.old.debug.DebugHandler;
import me.lokka30.levelledmobs.plugin.old.file.FileHandler;
import me.lokka30.levelledmobs.plugin.old.integration.IntegrationHandler;
import me.lokka30.levelledmobs.plugin.old.level.LevelHandler;
import me.lokka30.levelledmobs.plugin.old.listener.BlockPlaceListener;
import me.lokka30.levelledmobs.plugin.old.listener.ChunkLoadListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityCombustListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityDamageByEntityListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityDamageListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityDeathListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityRegainHealthListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntitySpawnListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityTameListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityTargetListener;
import me.lokka30.levelledmobs.plugin.old.listener.EntityTransformListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerChangedWorldListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerDeathListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerInteractEntityListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerInteractListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerJoinListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerQuitListener;
import me.lokka30.levelledmobs.plugin.old.listener.PlayerTeleportListener;
import me.lokka30.levelledmobs.plugin.old.metric.MetricsHandler;
import me.lokka30.levelledmobs.plugin.old.nametag.NametagHandler;
import me.lokka30.levelledmobs.plugin.old.nms.NMSHandler;
import me.lokka30.levelledmobs.plugin.old.queue.QueueHandler;
import me.lokka30.levelledmobs.plugin.old.translation.TranslationHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of the plugin. Acts as a 'hub' of sorts in the plugin's code.
 *
 * @author lokka30
 * @since 4.0.0
 */
public final class LevelledMobs extends JavaPlugin {

    private static LevelledMobs instance;

    public static LevelledMobs getInstance() {
        return instance;
    }

    public static Logger logger() {
        return instance.getLogger();
    }

    public final QueueHandler queueHandler = new QueueHandler();
    public final TranslationHandler translationHandler = new TranslationHandler();
    public final NametagHandler nametagHandler = new NametagHandler();
    public final MetricsHandler metricsHandler = new MetricsHandler();
    public final LevelHandler levelHandler = new LevelHandler();
    public final IntegrationHandler integrationHandler = new IntegrationHandler();
    public final DebugHandler debugHandler = new DebugHandler();
    public final CustomDropHandler customDropHandler = new CustomDropHandler();
    public final FileHandler fileHandler = new FileHandler();
    public final NMSHandler nmsHandler = new NMSHandler();

    /* Start-up & shut-down methods */

    /**
     * Called by Bukkit's plugin manager in the 'loading' stage of the server. This runs before
     * 'onEnable', so any important things to get done before 'onEnable' must be added here.
     *
     * @author lokka30
     * @since 4.0.0
     */
    @Override
    public void onLoad() {
        final var startTime = System.currentTimeMillis();

        instance = this;
        //TODO lokka30: Complete this method's body.


        getLogger().info(
            "Plugin initialized (took " + (System.currentTimeMillis() - startTime) + "ms).");
    }

    /**
     * Called by Bukkit's plugin manager when it enables the plugin. Ensure reloads are factored in
     * to any code ran inside this method. Warning: Methods are ordered on purpose, as some code
     * requires other code to be ran first (e.g. listeners require configs to be loaded first).
     *
     * @author lokka30
     * @since 4.0.0
     */
    @Override
    public void onEnable() {
        final var startTime = System.currentTimeMillis();

        // IMPORTANT: Do not adjust the order of these methods being ran!

        //TODO lokka30: Complete this method's body.
        fileHandler.loadAll(false);

        debugHandler.load();

        queueHandler.startQueues();

        loadEventListeners();

        getLogger().info(
            "Plugin enabled (took " + (System.currentTimeMillis() - startTime) + "ms).");
    }

    /**
     * @author lokka30
     * @since 4.0.0 Called by Bukkit's plugin manager when it enables the plugin. Ensure reloads are
     * factored in to any code ran inside this method.
     */
    @Override
    public void onDisable() {
        final var startTime = System.currentTimeMillis();

        // IMPORTANT: Do not mess with the order of these methods being ran!

        //TODO lokka30: Complete this method's body.

        queueHandler.stopQueues();

        getLogger().info(
            "Plugin disabled (took " + (System.currentTimeMillis() - startTime) + "ms).");
    }

    /* Methods ran by onLoad, onEnable and onDisable */

    /**
     * @author lokka30
     * @see me.lokka30.levelledmobs.plugin.bukkit.listener
     * @see org.bukkit.plugin.PluginManager#registerEvents(Listener, Plugin)
     * @since 4.0.0 Registers ALL of LevelledMobs' listener classes through Bukkit's plugin manager.
     * Only to be ran from onEnable, do not use elsewhere. The HashSet of Listeners must be updated
     * manually if a new Listener is added to LM.
     */
    private void loadEventListeners() {
        getLogger().info("Loading event listeners...");

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
        ).forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
    }
}
