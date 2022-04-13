/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.integration;

import java.util.HashSet;
import java.util.List;
import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.BossIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.CitizensIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.DangerousCavesIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.EcoBossesIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.EliteMobsIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.InfernalMobsIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.MythicMobsIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.NBTAPIIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.PlaceholderAPIIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.ShopkeepersIntegration;
import me.lokka30.levelledmobs.plugin.bukkit.integration.internal.WorldGuardIntegration;
import org.jetbrains.annotations.NotNull;

/**
 * This class handles all internal and external integrations utilized in LevelledMobs.
 *
 * @author lokka30
 * @since 4.0.0
 */
public class IntegrationHandler {

    private final HashSet<Integration> availableIntegrations = new HashSet<>();

    @NotNull
    public HashSet<Integration> getAvailableIntegrations() {
        return availableIntegrations;
    }

    /**
     * Adds the specified integration to the list of available integrations. An available
     * integration can also be a force-disabled one. The integration doesn't need to be enabled to
     * exist in the set, it just needs to exist.
     *
     * @param integration what integration to add to the list
     * @since 4.0.0
     */
    public void addIntegration(final Integration integration) {
        // Make sure there are no duplicate entries
        if(getAvailableIntegrations().contains(integration)) {
            LevelledMobs.logger().warning(
                "An attempt was made to add the integration '&b" + integration + "&7', but it is " +
                    "already added. This may be an error with an external integration or an internal error. Please "
                    +
                    "inform the LevelledMobs team.");
            return;
        }

        // Add integration to the available integrations set.
        getAvailableIntegrations().add(integration);
    }

    /**
     * This method adds any standard integrations (ones that exist in LevelledMobs, not ones added
     * by other plugins) to the Available Integrations set.
     *
     * @since 4.0.0
     */
    public void loadDefaultIntegrations() {
        List.of(
            new BossIntegration(),
            new CitizensIntegration(),
            new DangerousCavesIntegration(),
            new EcoBossesIntegration(),
            new EliteMobsIntegration(),
            new InfernalMobsIntegration(),
            new MythicMobsIntegration(),
            new NBTAPIIntegration(),
            new PlaceholderAPIIntegration(),
            new ShopkeepersIntegration(),
            new WorldGuardIntegration()
        ).forEach(this::addIntegration);
    }
}
