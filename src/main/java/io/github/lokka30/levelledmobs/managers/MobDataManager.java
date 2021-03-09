package io.github.lokka30.levelledmobs.managers;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.misc.Addition;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

/**
 * @author lokka30
 */
public class MobDataManager {

    private final LevelledMobs instance;

    public MobDataManager(final LevelledMobs instance) {
        this.instance = instance;
    }

    public Object getAttributeDefaultValue(final EntityType entityType, final Attribute attribute) {
        final String path = entityType.toString() + "." + attribute.toString();

        if (instance.attributesCfg.contains(path)) {
            return instance.attributesCfg.get(path);
        } else {
            return null;
        }
    }

    public final boolean isLevelledDropManaged(final EntityType entityType, final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!instance.settingsCfg.getBoolean("mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return instance.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    public void setAdditionsForLevel(final LivingEntity livingEntity, final Attribute attribute, final Addition addition, final int currentLevel, final boolean useBaseValue) {
        final double defaultValue = useBaseValue ?
                Objects.requireNonNull(livingEntity.getAttribute(attribute)).getBaseValue () :
                (double) getAttributeDefaultValue(livingEntity.getType(), attribute);
        double tempValue = defaultValue + getAdditionsForLevel(livingEntity, addition, currentLevel);
        if (attribute.equals(Attribute.GENERIC_MAX_HEALTH) && tempValue > 2048.0){
            // max health has hard limit of 2048
            tempValue = 2048.0;
        }
        Objects.requireNonNull(livingEntity.getAttribute(attribute)).setBaseValue(tempValue);
    }

    public final double getAdditionsForLevel(final LivingEntity livingEntity, final Addition addition, final int currentLevel) {
        final int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level");
        final int maxLevel = instance.settingsCfg.getInt("fine-tuning.max-level");
        final double range = (double) maxLevel - minLevel - 1;
        final double percent = (double) currentLevel / range;

        final boolean isAdult = !(livingEntity instanceof Ageable) || ((Ageable)livingEntity).isAdult();
        final String entityCheckName = isAdult ?
                livingEntity.getName().replace(" ", "_").toUpperCase() :
                "BABY_" + livingEntity.getName().replace(" ", "_").toUpperCase();
        final double maxOverridenEntity = instance.settingsCfg.getDouble(addition.getMaxAdditionConfigPath(entityCheckName), -100.0); // in case negative number are allowed
        final double max = instance.settingsCfg.getDouble(addition.getMaxAdditionConfigPath());
        //Utils.logger.info(String.format("cl: %s, lmin: %s, lmax: %s, max: %s, percent: %s, test: %s", currentLevel, minLevel, maxLevel, max, percent, addition.getMaxAdditionConfigPath()));

        return maxOverridenEntity > -100.0 ?
                percent * maxOverridenEntity :
                percent * max;
    }
}
