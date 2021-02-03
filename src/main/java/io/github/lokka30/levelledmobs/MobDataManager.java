package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.utils.Addition;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

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

    public void setAdditionsForLevel(final LivingEntity livingEntity, final Attribute attribute, final Addition addition, final int currentLevel) {
        final double defaultValue = (double) getAttributeDefaultValue(livingEntity.getType(), attribute);
        Objects.requireNonNull(livingEntity.getAttribute(attribute)).setBaseValue(defaultValue + getAdditionsForLevel(livingEntity, addition, currentLevel));
    }

    public final double getAdditionsForLevel(final LivingEntity livingEntity, final Addition addition, final int currentLevel) {
        final int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level");
        final int maxLevel = instance.settingsCfg.getInt("fine-tuning.max-level");
        final double range = (double) maxLevel - minLevel - 1;
        final double percent = (double) currentLevel / range;

        final boolean isAdult = !(livingEntity instanceof Ageable) || ((Ageable)livingEntity).isAdult();
        final String entityCheckName = isAdult ? livingEntity.getName() : "BABY_" + livingEntity.getName();
        final double maxOverridenEntity = instance.settingsCfg.getDouble(addition.getMaxAdditionConfigPath(entityCheckName), -100.0); // in case negative number are allowed
        final double max = instance.settingsCfg.getDouble(addition.getMaxAdditionConfigPath());
        //Utils.logger.info(String.format("cl: %s, lmin: %s, lmax: %s, max: %s, percent: %s, test: %s", currentLevel, minLevel, maxLevel, max, percent, addition.getMaxAdditionConfigPath()));

        return maxOverridenEntity > -100.0 ?
                percent * maxOverridenEntity :
                percent * max;
    }
}
