package io.github.lokka30.levelledmobs.misc;

/**
 * @author lokka30
 */
public enum Addition {

    // Prefix of ATTRIBUTE if it is a Minecraft vanilla attribute like GENERIC_MOVEMENT_SPEED
    ATTRIBUTE_MOVEMENT_SPEED("movement-speed"),
    ATTRIBUTE_ATTACK_DAMAGE("attack-damage"),
    ATTRIBUTE_MAX_HEALTH("max-health"),

    // Prefix of CUSTOM if it is a custom value used in listeners
    CUSTOM_RANGED_ATTACK_DAMAGE("ranged-attack-damage"),
    CUSTOM_ITEM_DROP("item-drop"),
    CUSTOM_XP_DROP("xp-drop");

    private final String maxAdditionConfigPath;
    private final static String fineTuningBase = "fine-tuning.additions.";

    Addition(String maxAdditionConfigPath) {
        this.maxAdditionConfigPath = maxAdditionConfigPath;
    }

    public String getMaxAdditionConfigPath() {
        return fineTuningBase + maxAdditionConfigPath;
    }

    public String getMaxAdditionConfigPath(String entityName) {
        // fine-tuning.additions.custom_mob_level.entityName.max_whatever
        return fineTuningBase + "custom_mob_level." + entityName.toUpperCase() + "." + maxAdditionConfigPath;
    }
}
