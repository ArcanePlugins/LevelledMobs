package io.github.lokka30.levelledmobs;

import io.github.lokka30.levelledmobs.utils.Addition;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

public class MobDataManager {

    private final LevelledMobs instance;

    public MobDataManager(final LevelledMobs instance) {
        this.instance = instance;
    }

    public Object getAttributeDefaultValue(EntityType entityType, Attribute attribute) {
        String path = entityType.toString() + "." + attribute.toString();

        if (instance.attributesCfg.contains(path)) {
            return instance.attributesCfg.get(path);
        } else {
            return null;
        }
    }

    public final boolean isLevelledDropManaged(EntityType entityType, Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!instance.settingsCfg.getBoolean("mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return instance.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    public void setAdditionsForLevel(LivingEntity livingEntity, Attribute attribute, Addition addition, int currentLevel) {
        final double defaultValue = (double) getAttributeDefaultValue(livingEntity.getType(), attribute);
        Objects.requireNonNull(livingEntity.getAttribute(attribute)).setBaseValue(defaultValue + getAdditionsForLevel(livingEntity, addition, currentLevel));
    }

    public final double getAdditionsForLevel(LivingEntity livingEntity, Addition addition, int currentLevel) {
        final int minLevel = instance.settingsCfg.getInt("fine-tuning.min-level");
        final int maxLevel = instance.settingsCfg.getInt("fine-tuning.max-level");
        final double range = (double) maxLevel - (minLevel - 1);
        final double percent = currentLevel / range;
        final double max = instance.settingsCfg.getDouble(addition.getMaxAdditionConfigPath());
        return percent * max;
    }
}
