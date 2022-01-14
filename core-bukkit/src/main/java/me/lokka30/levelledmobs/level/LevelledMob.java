/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.level;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * This class makes it easier to access
 * aspects about a levelled mob, such
 * as what level they are.
 *
 * @author lokka30
 * @since v4.0.0
 */
public class LevelledMob {

    private final LivingEntity livingEntity;
    public LevelledMob(
            final @NotNull LivingEntity livingEntity
    ) {
        this.livingEntity = livingEntity;

        assert isEntityLevelled(livingEntity);
    }

    @NotNull
    public LivingEntity getLivingEntity() { return livingEntity; }

    /*
    TODO
        lokka30: Complete class with methods of course.
     */

    public synchronized int getLevel() {
        return livingEntity.getPersistentDataContainer().get(
                LevelledMobs.getInstance().getLevelHandler().getLevelledNamespacedKeys().getLevelKey(),
                PersistentDataType.INTEGER
        );
    }

    public synchronized void setLevel(final int newLevel) {
        if(newLevel < 1) { removeLevel(); return; }

        getPDC().set(
                LevelledMobs.getInstance().getLevelHandler().getLevelledNamespacedKeys().getLevelKey(),
                PersistentDataType.INTEGER,
                newLevel
        );

        /*
        TODO
        attributes
        nametags
        javadoc
         */

    }

    public synchronized void removeLevel() {
        LevelledMobs.getInstance().getLevelHandler().getLevelledNamespacedKeys().getAllKeys().forEach(getPDC()::remove);
    }

    @NotNull
    public synchronized String getNametagFormat() {
        return getPDC().get(
                LevelledMobs.getInstance().getLevelHandler().getLevelledNamespacedKeys().getNametagFormatKey(),
                PersistentDataType.STRING
        );
    }

    public synchronized static boolean isEntityLevelled(final @NotNull LivingEntity livingEntity) {
        return livingEntity.getPersistentDataContainer().has(
                LevelledMobs.getInstance().getLevelHandler().getLevelledNamespacedKeys().getLevelKey(),
                PersistentDataType.INTEGER
        );
    }

    @NotNull
    public PersistentDataContainer getPDC(){
        return this.livingEntity.getPersistentDataContainer();
    }
}
