package io.github.arcaneplugins.levelledmobs.bukkit.debug

enum class DebugCategory {

    /**
     * Displays a bunch of statistics about a levelled mob when hit with a melee.
     * Inspecting player requires the `levelledmobs.debug` permission (default=op).
     */
    ENTITY_INSPECTOR,

    /**
     * Shows the buff application process for [SetBuffsAction].
     */
    BUFFS,

    /**
     * Shows information about drop calculation in [EntityDeathListener].
     */
    DROPS_GENERIC,

    DROPS_FILTRATION_BY_GROUP,

    /**
     * Shows information about logic in the [SpawnDistanceLevellingStrategy].
     */
    SPAWN_DISTANCE_STRATEGY,

    /**
     * Shows information about logic in the [YAxisLevellingStrategy].
     *
     * @since 4.0.0
     */
    Y_AXIS_STRATEGY,

    /**
     * Generic debug category, not recommended for use outside temporary testing.
     *
     * @since 4.0.0
     */
    UNKNOWN,

    /**
     * Shows various logic happening in the background for packet labels
     */
    PACKET_LABELS,

    /**
     * When this debug category is enabled, debug logs are also broadcasted in chat to
     * server operators, in addition to the server console (as per normal).
     */
    BROADCAST_TO_OPS,

    /**
     * Generic debug logs related to listeners.
     */
    LISTENERS,

    /**
     * Generic debug logs related to LmFunctions.
     */
    FUNCTIONS_GENERIC,

    /**
     * When enabled, every time a condition is evaluated it will show
     * the result and context
     */
    CONDITION
}