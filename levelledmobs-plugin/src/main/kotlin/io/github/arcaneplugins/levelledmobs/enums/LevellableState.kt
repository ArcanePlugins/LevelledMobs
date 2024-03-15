package io.github.arcaneplugins.levelledmobs.enums

/**
 * This provides information on if a mob is levellable or not, and if not, a reason is supplied. A
 * mob is levellable if their LevellableState = ALLOW.
 *
 * @author lokka30
 * @since 2.4.0
 */
enum class LevellableState {
    /**
     * The entity is ALLOWED to be levelled. Note to developers: there must only be one 'ALLOWED'
     * constant.
     */
    ALLOWED,

    /**
     * the plugin force blocked an entity type, such as a PLAYER or ARMOR STAND which are not meant
     * to be 'levelled mobs'.
     */
    DENIED_FORCE_BLOCKED_ENTITY_TYPE,

    /**
     * settings.yml has been configured to block mobs of such entity type from being levelled
     */
    DENIED_CONFIGURATION_BLOCKED_ENTITY_TYPE,

    /**
     * A rule has been configured to block MythicMobs mobs from being levelled.
     */
    DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS,

    /**
     * A rule has been configured to block EliteMobs mobs from being levelled.
     */
    DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS,

    /**
     * A rule has been configured to block Simple Pets from being levelled
     */
    DENIED_CONFIGURATION_COMPATIBILITY_SIMPLEPETS,

    /**
     * A rule has been configured to block Elite Bosses from being levelled
     */
    DENIED_CONFIGURATION_COMPATIBILITY_ELITE_BOSSES,


    DENIED_EXTERNAL_PLUGIN,

    /**
     * A rule has been configured to block nametagged mobs from being levelled.
     */
    DENIED_CONFIGURATION_CONDITION_NAMETAGGED,

    /**
     * If no rules in the rule list applied to the mob then it will be denied
     */
    DENIED_NO_APPLICABLE_RULES,

    /**
     * When a reason is not applicable, use this. Please contact a lead developer if you believe you
     * must resort to using this.
     */
    DENIED_OTHER,

    DENIED_LEVEL_0
}