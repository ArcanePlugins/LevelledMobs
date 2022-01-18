package me.lokka30.levelledmobs.level;

import me.lokka30.levelledmobs.event.MobPostLevelEvent;
import me.lokka30.levelledmobs.event.MobPreLevelEvent;

/**
 * When a mob is levelled, a set of these constants
 * can be provided to describe some circumstances
 * about the mob being levelled.
 * TODO add description.
 *
 * @author stumper66, lokka30
 * @since 3.1.0
 * @see MobPreLevelEvent
 * @see MobPostLevelEvent
 */
public enum AdditionalLevelInformation {

    /**
     * The mob was spawned in via LM's summon subcommand.
     *
     * @since 4.0.0
     */
    SUMMONED
}
