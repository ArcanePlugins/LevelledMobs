/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.handlers.FileHandler;
import me.lokka30.levelledmobs.handlers.IntegrationHandler;
import me.lokka30.levelledmobs.handlers.LevelHandler;
import me.lokka30.levelledmobs.handlers.StaticMobDataHandler;
import me.lokka30.levelledmobs.listeners.*;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.maths.QuickTimer;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author lokka30
 * @since v4.0.0
 * Main class of the plugin.
 */
public class LevelledMobs extends JavaPlugin {

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
        final QuickTimer timer = new QuickTimer();
        Utils.LOGGER.info("&3Start-up: &f~ Initiating pre-start-up sequence ~");

        //TODO lokka30: Complete this method's body.

        Utils.LOGGER.info("&3Start-up: &f~ Pre-start-up complete, took &b" + timer.getTimer() + "ms&f ~");
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
        Utils.LOGGER.info("&3Start-up: &f~ Initiating start-up sequence ~");

        // IMPORTANT: Do not mess with the order of these methods being ran!

        //TODO lokka30: Complete this method's body.
        fileHandler.loadInternalFiles();
        fileHandler.loadExternalFiles();
        loadListeners();

        Utils.LOGGER.info("&3Start-up: &f~ Start-up complete, took &b" + timer.getTimer() + "ms&f ~");
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
        Utils.LOGGER.info("&3Shut-down: &f~ Initiating shut-down sequence ~");

        // IMPORTANT: Do not mess with the order of these methods being ran!

        //TODO lokka30: Complete this method's body.

        Utils.LOGGER.info("&3Shut-down: &f~ Shut-down complete, took &b" + timer.getTimer() + "ms&f ~");
    }

    /* Methods ran by onLoad, onEnable and onDisable */

    /**
     * @author lokka30
     * @see me.lokka30.levelledmobs.listeners
     * @see org.bukkit.plugin.PluginManager#registerEvents(Listener, Plugin)
     * @since v4.0.0
     * Registers ALL of LevelledMobs' listener classes through Bukkit's plugin manager.
     * Only to be ran from onEnable, do not use elsewhere.
     * The HashSet of Listeners must be updated manually if a new Listener is added to LM.
     */
    private void loadListeners() {
        Utils.LOGGER.info("&3Start-up: &7Loading listeners...");

        final HashSet<Listener> listeners = new HashSet<>(Arrays.asList(
                new BlockPlaceListener(this),
                new ChunkLoadListener(this),
                new EntityCombustListener(this),
                new EntityDamageByEntityListener(this),
                new EntityDamageListener(this),
                new EntityDeathListener(this),
                new EntityRegainHealthListener(this),
                new EntitySpawnListener(this),
                new EntityTameListener(this),
                new EntityTargetListener(this),
                new EntityTransformListener(this),
                new PlayerChangedWorldListener(this),
                new PlayerDeathListener(this),
                new PlayerInteractEntityListener(this),
                new PlayerInteractListener(this),
                new PlayerJoinListener(this),
                new PlayerQuitListener(this),
                new PlayerTeleportListener(this)
        ));

        listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

    /* Instances of handler classes to be used across the plugin and by other plugins too */

    @NotNull
    public final FileHandler fileHandler = new FileHandler(this);
    @NotNull
    public final LevelHandler levelHandler = new LevelHandler(this);
    @NotNull
    public final IntegrationHandler integrationHandler = new IntegrationHandler(this);
    @NotNull
    public final StaticMobDataHandler staticMobDataHandler = new StaticMobDataHandler(this);
}
