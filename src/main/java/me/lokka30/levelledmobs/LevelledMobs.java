/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.handlers.IntegrationHandler;
import me.lokka30.levelledmobs.handlers.LevelHandler;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.maths.QuickTimer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 */
public class LevelledMobs extends JavaPlugin {

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
     */
    @Override
    public void onEnable() {
        final QuickTimer timer = new QuickTimer();
        Utils.LOGGER.info("&3Start-up: &f~ Initiating start-up sequence ~");

        //TODO lokka30: Complete this method's body.

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
        //TODO lokka30: Complete this method's body.
    }

    @NotNull
    private final LevelHandler levelHandler = new LevelHandler(this);

    @NotNull
    public LevelHandler getLevelHandler() {
        return levelHandler;
    }

    @NotNull
    private final IntegrationHandler integrationHandler = new IntegrationHandler(this);

    @NotNull
    public IntegrationHandler getIntegrationHandler() {
        return integrationHandler;
    }
}
