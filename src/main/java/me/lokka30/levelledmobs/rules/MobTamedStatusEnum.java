package me.lokka30.levelledmobs.rules;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public enum MobTamedStatusEnum {
    NOT_SPECIFIED,  // default
    TAMED,          // Mob must be tamed for the rule to work
    NOT_TAMED,      // Mob must be not tamed for the rule to work
    EITHER          // Doesn't matter what the tamed status of the mob is
}
