package me.lokka30.levelledmobs.rules;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public enum MobCustomNameStatusEnum {
    NOT_SPECIFIED,  // default
    NAMETAGGED,     // Mob must be nametagged for the rule to work
    NOT_NAMETAGGED, // Mob must be not nametagged for the rule to work
    EITHER          // Doesn't matter what the nametag status of the mob is
}
