package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class MobDataManager {

    private final LevelledMobs main;

    public MobDataManager(final LevelledMobs main) {
        this.main = main;
    }

    @Nullable
    public Object getAttributeDefaultValue(final EntityType entityType, final Attribute attribute) {
        final String path = entityType.toString() + "." + attribute.toString();

        if (main.attributesCfg.contains(path)) {
            return main.attributesCfg.get(path);
        } else {
            return null;
        }
    }

    public final boolean isLevelledDropManaged(final EntityType entityType, final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.settingsCfg.getBoolean("mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    public void setAdditionsForLevel(final LivingEntityWrapper lmEntity, final Attribute attribute, final Addition addition, final boolean useBaseValue) {
        double defaultValue = Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).getBaseValue();
        if (!useBaseValue){
            Object valueTemp = getAttributeDefaultValue(lmEntity.getLivingEntity().getType(), attribute);
            if (valueTemp != null) defaultValue = (double) valueTemp;
        }

        double tempValue = defaultValue + getAdditionsForLevel(lmEntity, addition);
        if (attribute.equals(Attribute.GENERIC_MAX_HEALTH) && tempValue > 2048.0){
            // max health has hard limit of 2048
            tempValue = 2048.0;
        }

        Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).setBaseValue(tempValue);
    }

    public final double getAdditionsForLevel(final LivingEntityWrapper lmEntity, final Addition addition) {
        final int minLevel = main.rulesManager.getRule_MobMinLevel(lmEntity);
        final int maxLevel = main.rulesManager.getRule_MobMaxLevel(lmEntity);
        final double range = (double) maxLevel - minLevel - 1;
        final double percent = (double) lmEntity.getMobLevel() / range;

        double attributeValue = 0;
        if (lmEntity.getFineTuningAttributes() != null){
            switch (addition){
                case CUSTOM_XP_DROP:
                    if (lmEntity.getFineTuningAttributes().xpDrop != null) attributeValue = lmEntity.getFineTuningAttributes().xpDrop;
                    break;
                case CUSTOM_ITEM_DROP:
                    if (lmEntity.getFineTuningAttributes().itemDrop != null) attributeValue = lmEntity.getFineTuningAttributes().itemDrop;
                    break;
                case ATTRIBUTE_MAX_HEALTH:
                    if (lmEntity.getFineTuningAttributes().maxHealth != null) attributeValue = lmEntity.getFineTuningAttributes().maxHealth;
                    break;
                case ATTRIBUTE_ATTACK_DAMAGE:
                    if (lmEntity.getFineTuningAttributes().attackDamage != null) attributeValue = lmEntity.getFineTuningAttributes().attackDamage;
                    break;
                case ATTRIBUTE_MOVEMENT_SPEED:
                    if (lmEntity.getFineTuningAttributes().movementSpeed != null) attributeValue = lmEntity.getFineTuningAttributes().movementSpeed;
                    break;
                case CUSTOM_RANGED_ATTACK_DAMAGE:
                    if (lmEntity.getFineTuningAttributes().rangedAttackDamage != null) attributeValue = lmEntity.getFineTuningAttributes().rangedAttackDamage;
                    break;
            }
        }

        Utils.logger.info("getAdditionsForLevel " + addition + ", percent: " + percent + ", value: " + attributeValue);

        return percent * attributeValue;
    }
}
