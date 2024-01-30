package io.github.arcaneplugins.levelledmobs.enums

/**
 * Holds values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
enum class MobTamedStatus {
    NOT_SPECIFIED,  // default
    TAMED,  // Mob must be tamed for the rule to work
    NOT_TAMED,  // Mob must be not tamed for the rule to work
    EITHER // Doesn't matter what the tamed status of the mob is
}