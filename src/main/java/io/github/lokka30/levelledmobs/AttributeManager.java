package io.github.lokka30.levelledmobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

public class AttributeManager {

    private final LevelledMobs instance;

    public AttributeManager(final LevelledMobs instance) {
        this.instance = instance;
    }

    public Object getDefaultValue(EntityType entityType, Attribute attribute) {
        String path = entityType.toString() + "." + attribute.toString();

        if (instance.attributesCfg.contains(path)) {
            return instance.attributesCfg.get(path);
        } else {
            return null;
        }
    }

    public void applyNewValue(LivingEntity entity, Attribute attribute, Double newValue) {
        Objects.requireNonNull(entity.getAttribute(attribute)).setBaseValue(newValue);
    }

    public void setMultipliedValue(LivingEntity entity, Attribute attribute, Double multiplier, int level) {
        double currentValue = (double) getDefaultValue(entity.getType(), attribute);
        applyNewValue(entity, attribute, currentValue * (multiplier * level));
    }
}
