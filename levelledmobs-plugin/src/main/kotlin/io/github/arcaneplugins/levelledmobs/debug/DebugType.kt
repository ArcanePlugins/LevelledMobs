package io.github.arcaneplugins.levelledmobs.debug

/**
 * Holds the enums used for showing debug data
 *
 * @author lokka30, stumper66
 * @since 2.5.0
 */
enum class DebugType {
    /**
     * Logged when LM is checking if a mob can have a level applied to it, and whether it was
     * successful
     */
    APPLY_LEVEL_RESULT,

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
    PL_UPDATE_NAMETAG,

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
    NBT_APPLICATION,

    /**
     * Logged when LM is processing a mob from a creature spawner
     */
    LM_MOB_SPAWNER,

    CONDITION_ENTITIES_LIST,

    CONDITION_MINLEVEL,

    CONDITION_MAXLEVEL,

    CONDITION_WORLD_LIST,

    CONDITION_BIOME_LIST,

    CONDITION_PLUGIN_COMPAT,

    CONDITION_SPAWN_REASON,

    CONDITION_CUSTOM_NAME,

    CONDITION_CHANCE,

    CONDITION_WG_REGION,

    CONDITION_WG_REGION_OWNER,

    CONDITION_Y_LEVEL,

    CONDITION_MIN_SPAWN_DISTANCE,
    CONDITION_MAX_SPAWN_DISTANCE,

    SETTING_STOP_PROCESSING,

    PLAYER_LEVELLING,

    CONDITION_MYTHICMOBS_INTERNAL_NAME,

    CONDITION_SPAWNER_NAME,

    CONDITION_WORLD_TIME_TICK,

    CONDITION_PERMISSION,

    APPLY_MULTIPLIERS,

    CUSTOM_DROPS,

    CUSTOM_EQUIPS,

    MOB_GROUPS,

    GROUP_LIMITS,

    THREAD_LOCKS,

    SCOREBOARD_TAGS,

    SKYLIGHT_LEVEL,

    CHUNK_KILL_COUNT,

    SETTING_COOLDOWN,

    REMOVED_MULTIPLIERS,

    CONDITION_WITH_COORDINATES,

    MOB_HASH,

    DEVELOPER_LEW_CACHE,

    CUSTOM_STRATEGY,

    CONSTRUCT_LEVEL,

    STRATEGY_RESULT
}
