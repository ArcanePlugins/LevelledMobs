/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.level;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * This class contains a bunch of NamespacedKeys
 * that can be accessed by both LevelledMobs and any other
 * plugin that wishes to check &/or manage these Keys.
 * ALL namespaced keys in this class MUST be registered.
 *
 * @author lokka30
 * @since 4.0.0
 * @see NamespacedKey
 */
public final class LevelledNamespacedKeys {

    public LevelledNamespacedKeys() {
        final LevelledMobs main = LevelledMobs.getInstance();

        /* Register the namespaced keys */
        levelKey = new NamespacedKey(main, "level");
        spawnReasonKey = new NamespacedKey(main, "spawnReason");
        wasBabyMobKey = new NamespacedKey(main, "wasBabyMob");
        overridenEntityNameKey = new NamespacedKey(main, "overridenEntityName");
        playerLevellingClosestPlayerUUIDKey = new NamespacedKey(main, "playerLevelling_Id");
        chanceRuleAllowedKey = new NamespacedKey(main, "chanceRule_Allowed");
        chanceRuleDeniedKey = new NamespacedKey(main, "chanceRule_Denied");
        nametagFormatKey = new NamespacedKey(main, "nametag-format");
        /*
        TODO Convert underscore IDs to non-underscore IDs
         */

        allKeys = Set.of(
                levelKey, spawnReasonKey, wasBabyMobKey, overridenEntityNameKey,
                playerLevellingClosestPlayerUUIDKey, chanceRuleAllowedKey, chanceRuleDeniedKey,
                nametagFormatKey
        );
    }

    /*
    TODO Javadoc.
     */
    private final Set<NamespacedKey> allKeys;
    public @NotNull Set<NamespacedKey> getAllKeys() { return allKeys; }

    /**
     * @since 4.0.0
     * What level a levelled mob is (e.g., `52`)
     * Type: Integer
     */
    private final NamespacedKey levelKey;
    public @NotNull NamespacedKey getLevelKey() { return levelKey; }

    /**
     * @since 4.0.0
     * Says how a mob was spawned
     * Type: String
     */
    private final NamespacedKey spawnReasonKey;
    public @NotNull NamespacedKey getSpawnReasonKey() { return spawnReasonKey; }

    /**
     * @since 4.0.0
     * States if the mob was a baby mob or not.
     * Type: Boolean
     */
    private final NamespacedKey wasBabyMobKey;
    public @NotNull NamespacedKey getWasBabyMobKey() { return wasBabyMobKey; }

    /**
     * @since 4.0.0
     * Set if a mob has an overriden entity name. If they do
     * then the entity name they were given is set here.
     * Type: String
     */
    private final NamespacedKey overridenEntityNameKey;
    public @NotNull NamespacedKey getOverridenEntityNameKey() { return overridenEntityNameKey; }

    /**
     * @since 4.0.0
     * For player levelling, the UUID of the closest mob to a player
     * is stored in the mob
     * Type: String
     */
    private final NamespacedKey playerLevellingClosestPlayerUUIDKey;
    public @NotNull NamespacedKey getPlayerLevellingClosestPlayerUUIDKey() { return playerLevellingClosestPlayerUUIDKey; }

    /**
     * @since 4.0.0
     * If a mob was processed through a rule that has a chance
     * and initially allowed the chance to happen (so the rule
     * became in effect), then the mob will have this key applied
     * to them along with the name of the rule.
     * If there are multiple chance-rules at once, the last one
     * applied wins this key.
     * Type: String
     */
    private final NamespacedKey chanceRuleAllowedKey;
    public @NotNull NamespacedKey getChanceRuleAllowedKey() { return chanceRuleAllowedKey; }

    /**
     * @since 4.0.0
     * Same as chanceRuleAllowed except that this key stores
     * chance rules that were denied upon a mob, instead of allowed.
     * Type: String
     */
    private final NamespacedKey chanceRuleDeniedKey;
    public @NotNull NamespacedKey getChanceRuleDeniedKey() { return chanceRuleDeniedKey; }

    /* TODO Javadoc */
    private final NamespacedKey nametagFormatKey;
    public @NotNull NamespacedKey getNametagFormatKey() { return nametagFormatKey; }
}
