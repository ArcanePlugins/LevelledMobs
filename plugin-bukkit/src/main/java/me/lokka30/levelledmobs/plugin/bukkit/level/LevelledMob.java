/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.level;

import me.lokka30.levelledmobs.api.bukkit.old.MobData;
import me.lokka30.levelledmobs.api.bukkit.old.util.NamespacedKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class makes it easier to access aspects about a levelled mob, such as what level they are.
 *
 * @author lokka30
 * @since 4.0.0
 */
public class LevelledMob {

    public final LivingEntity livingEntity;

    public LevelledMob(
        final @NotNull LivingEntity livingEntity
    ) {
        this.livingEntity = livingEntity;

        assert isEntityLevelled(livingEntity);
    }

    /*
    TODO
        lokka30: Complete class with methods of course.
     */

    public synchronized int getLevel() {
        return (int) Default.of(MobData.getLevel(livingEntity).get(), 0);
    }

    public synchronized void setLevel(final int newLevel) {
        if (newLevel < 1) {
            removeLevel();
            return;
        }

        getPDC().set(NamespacedKeys.LEVEL_KEY, PersistentDataType.INTEGER, newLevel);

        /*
        TODO
        attributes
        nametags
        javadoc
         */

    }

    public synchronized void removeLevel() {
        for(NamespacedKey key : NamespacedKeys.ALL_KEYS) { getPDC().remove(key); }
    }

    public synchronized void freezeLevelState() {
        getPDC().set(NamespacedKeys.FROZEN_LEVEL_STATE, PersistentDataType.STRING, Boolean.toString(true));
    }

    public synchronized void unfreezeLevelState() {
        getPDC().set(NamespacedKeys.FROZEN_LEVEL_STATE, PersistentDataType.STRING, Boolean.toString(false));
    }

    public synchronized boolean getLevelFrozenStatus() {
        return Boolean.parseBoolean(
            (String) Default.of(MobData.getFrozenLevelState(livingEntity).get(), Boolean.toString(false))
        );
    }

    @NotNull
    public synchronized String getNametagFormat() {
        return (String) Default.of(getPDC().get(NamespacedKeys.NAMETAG_FORMAT_KEY, PersistentDataType.STRING), "");
    }

    public synchronized static boolean isEntityLevelled(final @NotNull LivingEntity livingEntity) {
        return livingEntity.getPersistentDataContainer().has(NamespacedKeys.LEVEL_KEY, PersistentDataType.INTEGER);
    }

    @NotNull
    public PersistentDataContainer getPDC() {
        return this.livingEntity.getPersistentDataContainer();
    }

    public static class Default {

        public static Object of(@Nullable Object actualValue, @NotNull Object defaultValue) {
            return actualValue == null ? defaultValue : actualValue;
        }

    }

}
