package io.github.arcaneplugins.levelledmobs.rules

import java.util.TreeSet

object KeyValidation {
    val mainRuleSection = mutableSetOf(
        "is-enabled",
        "strategies",
        "conditions",
        "settings",
        "modifiers",
        "name",
        "use-preset",
        "custom-rule"
    )

    val settings: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val conditions: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val modifiers: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val strategies: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)

    init {
        settings.addAll(mutableSetOf(
            "attribute-modifier",
            "baby-mobs-inherit-adult-setting",
            "chunk-max-cooldown-seconds",
            "chunk-max-cooldown-seconds",
            "construct-level",
            "creature-death-nametag",
            "creeper-max-damage-radius",
            "death-messages",
            "disable-item-boost-on-chunk-max",
            "disable-vanilla-drops-on-chunk-max",
            "disable-xp-boost-on-chunk-max",
            "entity-name-override",
            "health-indicator",
            "level-inheritance",
            "lock-entity",
            "max-adjacent-chunks",
            "max-adjacent-chunks",
            "maximum-death-in-chunk-threshold",
            "maximum-death-in-chunk-threshold",
            "maxlevel",
            "minlevel",
            "multipliers",
            "nametag",
            "nametag-placeholder-levelled",
            "nametag-placeholder-unlevelled",
            "nametag-visibility-method",
            "nametag-visible-time",
            "no-drop-multipler-entities",
            "passenger-match-level",
            "spawner-particles",
            "spawner-particles-count",
            "sunlight-intensity",
            "tiered-coloring",
            "use-custom-item-drops-for-mobs",
            "use-droptable-id",

            // these values are not valid but they were present in rules.yml so long
            // we are leaving them so people don't get errors
            "transforming-mobs-inherit-level",
            "riding-passengers-match-vehicle-level"
        ))

        conditions.addAll(mutableSetOf(
            "apply-above-y",
            "apply-below-y",
            "biomes",
            "chance",
            "cooldown-duration",
            "cooldown-limit",
            "custom-names",
            "entities",
            "external-plugins",
            "max-distance-from-spawn",
            "maxlevel",
            "min-distance-from-spawn",
            "minlevel",
            "mob-customname-status",
            "mob-tamed-status",
            "mythicmobs-internal-names",
            "permission",
            "scoreboard-tags",
            "skylight-level",
            "spawner-egg-names",
            "spawner-names",
            "spawn-reasons",
            "stop-processing",
            "structures",
            "vanilla-bonus",
            "within-coordinates",
            "worldguard-region-owners",
            "worldguard-regions",
            "worlds",
            "world-time-tick"
        ))

        settings.addAll(mutableSetOf(
            "max-random-variance",
            "player-variable-mod"
        ))

        strategies.addAll(mutableSetOf(
            "distance-from-origin",
            "random",
            "weighted-random",
            "y-coordinate"
        ))
    }
}