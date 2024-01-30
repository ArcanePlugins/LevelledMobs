package io.github.arcaneplugins.levelledmobs.enums

/**
 * Holds values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
enum class MobCustomNameStatus {
    NOT_SPECIFIED,  // default
    NAMETAGGED,  // Mob must be nametagged for the rule to work
    NOT_NAMETAGGED,  // Mob must be not nametagged for the rule to work
    EITHER // Doesn't matter what the nametag status of the mob is
}
