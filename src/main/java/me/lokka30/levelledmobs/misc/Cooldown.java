/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

/**
 * This class is used to put cooldowns on certain actions,
 * on certain in-game objects - e.g. clicking entities or blocks.
 * <p>
 * For example, this is used in Debug Entity Damage to stop spam from
 * clicking the same entity over and over. It is also used in the
 * Spawner Info for the same reason: blocks unnecessary chat spam.
 *
 * @author lokka30
 * @see System#currentTimeMillis()
 * @since 3.1.2
 */
public class Cooldown {

    /**
     * The starting point of the cooldown - just run System#currentTimeMillis()
     */
    private final long startingTime;

    /**
     * What this cooldown should be identifiable by - e.g. entity ID, location
     */
    private final String identifier;

    public Cooldown(long startingTime, String identifier) {
        this.startingTime = startingTime;
        this.identifier = identifier;
    }

    /**
     * Checks if this cooldown belongs to something with a certain ID.
     *
     * @param identifier ID to check if this cooldown has the same ID.
     * @return if the IDs match.
     */
    public boolean doesCooldownBelongToIdentifier(String identifier) {
        return identifier.equals(this.identifier);
    }

    /**
     * Check if the cooldown's required time (seconds!) has surpassed since
     * the starting point.
     *
     * @param requiredTimeInSeconds how many seconds should the cooldown have lasted?
     * @return if the cooldown has expired.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasCooldownExpired(long requiredTimeInSeconds) {
        return ((System.currentTimeMillis() - startingTime) >= (requiredTimeInSeconds * 1000));
    }
}
