/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

/**
 * Holds the enums used for showing debug data
 *
 * @author lokka30, stumper66
 * @since 2.5.0
 */
public enum DebugType {
    /**
     * Logged when LM is checking if a mob can have a level applied to it, and whether it was
     * successful
     */
    APPLY_LEVEL_SUCCESS,
    APPLY_LEVEL_FAIL,

    /**
     * Logged when LM analyses a mob that spawns on the server.
     */
    ENTITY_SPAWN,

    /**
     * Logged when LM adjusts the ranged damage amounts from projectiles and guardians through
     * events (Minecraft doesn't have attributes for these)
     */
    RANGED_DAMAGE_MODIFICATION,

    CREEPER_BLAST_RADIUS,

    /**
     * Logged when LM processes a tamed entity, which may require re-levelling
     */
    ENTITY_TAME,

    /**
     * Logged when LM is adjusting the amount of drops a mob creates
     */
    SET_LEVELLED_ITEM_DROPS,

    SET_LEVELLED_XP_DROPS,

    /**
     * Logged when LM is adjusting the nametag of a levelled mob, but fails to do so
     */
    UPDATE_NAMETAG_FAIL,

    /**
     * Logged when LM is processing a entity tame event but is denied due to conditions
     */
    ENTITY_TRANSFORM_FAIL,

    /**
     * Logged when LM is adjusting the nametag of a levelled mob, and succeeds in doing so
     */
    UPDATE_NAMETAG_SUCCESS,

    /**
     * Misc events related to an entity
     */
    ENTITY_MISC,

    /**
     * When custom commands are being executed
     */
    CUSTOM_COMMANDS,

    /**
     * When applying NBT to a mob
     */
    NBT_APPLY_SUCCESS,

    /**
     * Logged when LM is processing a mob from a creature spawner
     **/
    MOB_SPAWNER,

    DENIED_RULE_ENTITIES_LIST,

    DENIED_RULE_MINLEVEL,

    DENIED_RULE_MAXLEVEL,

    DENIED_RULE_WORLD_LIST,

    DENIED_RULE_BIOME_LIST,

    DENIED_RULE_PLUGIN_COMPAT,

    DENIED_RULE_SPAWN_REASON,

    DENIED_RULE_CUSTOM_NAME,

    DENIED_RULE_CHANCE,

    DENIED_RULE_WG_REGION,

    DENIED_RULE_WG_REGION_OWNER,

    DENIED_RULE_Y_LEVEL,

    DENIED_RULE_MIN_SPAWN_DISTANCE,

    DENIED_RULE_MAX_SPAWN_DISTANCE,

    DENIED_RULE_STOP_PROCESSING,

    PLAYER_LEVELLING,

    DENIED_RULE_MYTHIC_MOBS_INTERNAL_NAME,

    DENIED_RULE_SPAWNER_NAME,

    DENIED_RULE_WORLD_TIME_TICK,

    DENIED_RULE_PERMISSION,

    ATTRIBUTE_MULTIPLIERS,

    CUSTOM_DROPS,

    THREAD_LOCKS,

    SCOREBOARD_TAGS,

    SKYLIGHT_LEVEL,

    CHUNK_KILL_COUNT,

    RULE_COOLDOWN
}
