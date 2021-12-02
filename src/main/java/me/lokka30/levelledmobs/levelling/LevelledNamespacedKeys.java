/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * This class contains a bunch of NamespacedKeys
 * that can be accessed by both LevelledMobs and any other
 * plugin that wishes to check &/or manage these Keys.
 * ALL namespaced keys in this class MUST be registered.
 *
 * @author lokka30
 * @since v4.0.0
 * @see NamespacedKey
 */
public class LevelledNamespacedKeys {

    public LevelledNamespacedKeys(@NotNull final LevelledMobs main) {
        /* Register the namespaced keys */
        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        playerLevellingClosestPlayerUUID = new NamespacedKey(main, "playerLevelling_Id");
        chanceRuleAllowed = new NamespacedKey(main, "chanceRule_Allowed");
        chanceRuleDenied = new NamespacedKey(main, "chanceRule_Denied");
    }

    /**
     * @since v4.0.0
     * What level a levelled mob is (e.g., `52`)
     * Type: Integer
     */
    @NotNull public final NamespacedKey levelKey;

    /**
     * @since v4.0.0
     * Says how a mob was spawned
     * Type: String
     */
    @NotNull public final NamespacedKey spawnReasonKey;

    /**
     * @since v4.0.0
     * States if the mob was a baby mob or not.
     * Type: Boolean
     */
    @NotNull public final NamespacedKey wasBabyMobKey;

    /**
     * @since v4.0.0
     * Set if a mob has an overriden entity name. If they do
     * then the entity name they were given is set here.
     * Type: String
     */
    @NotNull public final NamespacedKey overridenEntityNameKey;

    /**
     * @since v4.0.0
     * For player levelling, the UUID of the closest mob to a player
     * is stored in the mob
     * Type: String
     */
    @NotNull public final NamespacedKey playerLevellingClosestPlayerUUID;

    /**
     * @since v4.0.0
     * If a mob was processed through a rule that has a chance
     * and initially allowed the chance to happen (so the rule
     * became in effect), then the mob will have this key applied
     * to them along with the name of the rule.
     * If there are multiple chance-rules at once, the last one
     * applied wins this key.
     * Type: String
     */
    @NotNull public final NamespacedKey chanceRuleAllowed;

    /**
     * @since v4.0.0
     * Same as chanceRuleAllowed except that this key stores
     * chance rules that were denied upon a mob, instead of allowed.
     * Type: String
     */
    @NotNull public final NamespacedKey chanceRuleDenied;
}
