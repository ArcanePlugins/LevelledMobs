package io.github.lokka30.levelledmobs;

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

    public void applyAttributeNewValue(LivingEntity entity, Attribute attribute, Double newValue) {
        Objects.requireNonNull(entity.getAttribute(attribute)).setBaseValue(newValue);
    }

    public void setAttributeAddedValue(LivingEntity entity, Attribute attribute, Double addition, int level) {
        double defaultValue = (double) getAttributeDefaultValue(entity.getType(), attribute);
        applyAttributeNewValue(entity, attribute, defaultValue + (addition * level));
    }

    public boolean isLevelledDropManaged(EntityType entityType, Material material) {
        return instance.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }
}
