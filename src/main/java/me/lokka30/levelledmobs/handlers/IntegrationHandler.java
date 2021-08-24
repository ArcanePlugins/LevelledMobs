/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.handlers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.integrations.*;
import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles all of the Integrations
 * that LevelledMobs has.
 */
public class IntegrationHandler {

    private final LevelledMobs main;

    public IntegrationHandler(final LevelledMobs main) {
        this.main = main;
    }

    @NotNull
    private final HashSet<Integration> availableIntegrations = new HashSet<>();

    @NotNull
    public HashSet<Integration> getAvailableIntegrations() {
        return availableIntegrations;
    }

    /**
     * @param integration what integration to add to the list
     * @author lokka30
     * @since v4.0.0
     * Adds the specified integration to the list of available integrations.
     * An available integration can also be a force-disabled one. The integration
     * doesn't need to be enabled to exist in the set, it just needs to exist.
     */
    public void addIntegration(final Integration integration) {
        // Make sure there are no duplicate entries
        if (getAvailableIntegrations().contains(integration)) {
            Utils.LOGGER.warning("&3IntegrationHandler:&7 An attempt was made to make the integration '&b" + integration + "&7' available, although it is already available.");
            return;
        }

        // Add integration to the available integrations set.
        getAvailableIntegrations().add(integration);
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * This method adds any standard integrations (ones that
     * exist in LevelledMobs, not ones added by other plugins)
     * to the Available Integrations set.
     */
    public void loadDefaultIntegrations() {
        addIntegration(new AureliumMobsIntegration());
        addIntegration(new BossIntegration());
        addIntegration(new CitizensIntegration());
        addIntegration(new DangerousCavesIntegration());
        addIntegration(new EcoBossesIntegration());
        addIntegration(new EliteMobsIntegration());
        addIntegration(new InfernalMobsIntegration());
        addIntegration(new ItemsAdderIntegration());
        addIntegration(new LorinthsRPGMobsIntegration());
        addIntegration(new MythicMobsIntegration());
        addIntegration(new NBTAPIIntegration());
        addIntegration(new PlaceholderAPIIntegration());
        addIntegration(new ProtocolLibIntegration());
        addIntegration(new ShopkeepersIntegration());
        addIntegration(new WorldGuardIntegration());
    }
}
