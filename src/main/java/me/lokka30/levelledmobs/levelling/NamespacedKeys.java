/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @see NamespacedKey
 * @since v4.0.0
 * This class contains a bunch of NamespacedKeys
 * that can be accessed by both LevelledMobs and any other
 * plugin that wishes to check &/or manage these Keys.
 * ALL namespaced keys in this class MUST be registered.
 */
public class NamespacedKeys {

    public NamespacedKeys(final LevelledMobs main) {
        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        noLevelKey = new NamespacedKey(main, "noLevel");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        playerLevellingId = new NamespacedKey(main, "playerLevelling_Id");
        chanceRuleAllowed = new NamespacedKey(main, "chanceRule_Allowed");
        chanceRuleDenied = new NamespacedKey(main, "chanceRule_Denied");
    }

    /**
     * @since v4.0.0
     * What level a levelled mob is (e.g., `52`)
     */
    @NotNull public final NamespacedKey levelKey;

    /**
     * @since v4.0.0
     * Says how a mob was spawned
     */
    @NotNull public final NamespacedKey spawnReasonKey;

    @NotNull public final NamespacedKey noLevelKey;

    @NotNull public final NamespacedKey wasBabyMobKey;

    @NotNull public final NamespacedKey overridenEntityNameKey;

    @NotNull public final NamespacedKey playerLevellingId;

    @NotNull public final NamespacedKey chanceRuleAllowed;

    @NotNull public final NamespacedKey chanceRuleDenied;

    /*
    TODO
        lokka30: Add new namespaced-keys. Make sure they are registered - see NamespacedKeys#register(LevelledMobs).
     */
}
