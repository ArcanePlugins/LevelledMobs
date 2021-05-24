package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

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
    public Object getAttributeDefaultValue(@NotNull final LivingEntityWrapper lmEntity, final Attribute attribute) {
        if (lmEntity.getLivingEntity() instanceof Tameable && ((Tameable) lmEntity.getLivingEntity()).isTamed()){
            // if the tamed variant in the cfg then use it, otherwise check for untamed path
            final String tamedPath = "TAMED_" + lmEntity.getTypeName() + "." + attribute;
            if (main.attributesCfg.contains(tamedPath)) return main.attributesCfg.get(tamedPath);
        }

        final String path = lmEntity.getTypeName() + "." + attribute;

        return main.attributesCfg.contains(path) ?
            main.attributesCfg.get(path) :
            null;
    }

    public final boolean isLevelledDropManaged(final EntityType entityType, @NotNull final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.settingsCfg.getBoolean("mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    public void setAdditionsForLevel(@NotNull final LivingEntityWrapper lmEntity, final Attribute attribute, final Addition addition) {
        final boolean useStaticValues = main.settingsCfg.getBoolean("attributes-use-preset-base-values");
        double defaultValue = useStaticValues ?
                (double) Objects.requireNonNull(getAttributeDefaultValue(lmEntity, attribute)) :
                Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).getBaseValue();
        double additionValue = getAdditionsForLevel(lmEntity, addition, defaultValue);

        if (additionValue == 0.0) return;

        final AttributeModifier mod = new AttributeModifier("health", additionValue, AttributeModifier.Operation.ADD_NUMBER);
        final AttributeInstance attrib = lmEntity.getLivingEntity().getAttribute(attribute);

        if (attrib != null) {
            if (useStaticValues) attrib.setBaseValue(defaultValue);
            attrib.addModifier(mod);

            // MAX_HEALTH specific: set health to max health
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                lmEntity.getLivingEntity().setHealth(attrib.getValue());
            }
        }
    }

    public final double getAdditionsForLevel(final LivingEntityWrapper lmEntity, final Addition addition, double defaultValue) {
        final double maxLevel = main.rulesManager.getRule_MobMaxLevel(lmEntity);

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

        // use old formula or item drops, xp drops
        if (defaultValue == 0.0)
            return attributeValue * (((double) lmEntity.getMobLevel() - 1)/ maxLevel);

        // use revised formula for all attributes
        return (defaultValue * attributeValue) * ((lmEntity.getMobLevel() - 1) / maxLevel);
    }
}
