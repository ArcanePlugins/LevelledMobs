package me.lokka30.levelledmobs.misc;

/**
 * TODO Describe...
 *
 * @author lokka30
 * @contributors stumper66, UltimaOath, limzikiki
 */
public enum DebugType {
    /**
     * Logged when LM is checking if a mob
     * can have a level applied to it, and
     * whether it was successful
     */
    APPLY_LEVEL_SUCCESS,
    APPLY_LEVEL_FAIL,

    /**
     * Logged when LM analyses a mob that
     * spawns on the server.
     */
    ENTITY_SPAWN,

    /**
     * Logged when LM adjusts the ranged
     * damage amounts from projectiles and
     * guardians through events (Minecraft
     * doesn't have attributes for these)
     */
    RANGED_DAMAGE_MODIFICATION,

    /**
     * Logged when LM processes a tamed
     * entity, which may require re-levelling
     */
    ENTITY_TAME,

    /**
     * Logged when LM is adjusting the
     * amount of drops a mob creates
     */
    SET_LEVELLED_ITEM_DROPS,

    /**
     * Logged when LM is adjusting the
     * nametag of a levelled mob,
     * but fails to do so
     */
    UPDATE_NAMETAG_FAIL,

    /**
     * Logged when LM is processing
     * a entity tame event but is
     * denied due to conditions
     */
    ENTITY_TRANSFORM_FAIL,

    /**
     * Logged when LM is adjusting the
     * nametag of a levelled mob,
     * and succeeds in doing so
     */
    UPDATE_NAMETAG_SUCCESS,

    /**
     * Misc events related
     * to an entity
     */
    ENTITY_MISC,

    /**
     * When custom commands
     * are being executed
     */
    CUSTOM_COMMANDS
}
