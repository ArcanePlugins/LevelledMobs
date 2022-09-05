/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a mob or group instance and associates it with a list of custom drop items. This is where
 * the override for a mob / group is set
 *
 * @author stumper66
 * @since 2.4.0
 */
public class CustomDropInstance {

    public CustomDropInstance(final @Nullable EntityType associatedMob) {
        this.associatedMob = associatedMob;
        this.entityGroup = null;
        this.customItems = new LinkedList<>();
        this.overallPermissions = new LinkedList<>();
        this.isBabyMob = false;
    }

    CustomDropInstance(final @NotNull EntityType associatedMob, final boolean isBabyMob) {
        this.associatedMob = associatedMob;
        this.entityGroup = null;
        this.customItems = new LinkedList<>();
        this.overallPermissions = new LinkedList<>();
        this.isBabyMob = isBabyMob;
    }

    CustomDropInstance(final @NotNull CustomUniversalGroups entityGroup) {
        this.associatedMob = null;
        this.entityGroup = entityGroup;
        this.customItems = new LinkedList<>();
        this.overallPermissions = new LinkedList<>();
        isBabyMob = false;
    }

    private final @Nullable EntityType associatedMob;
    private final @Nullable CustomUniversalGroups entityGroup;
    public final @NotNull List<CustomDropBase> customItems;
    public Float overallChance;
    public final @NotNull List<String> overallPermissions;
    public boolean overrideStockDrops;
    boolean utilizesGroupIds;
    final boolean isBabyMob;

    public void combineDrop(final CustomDropInstance dropInstance) {
        if (dropInstance == null) {
            throw new NullPointerException("dropInstance");
        }

        if (dropInstance.overrideStockDrops) {
            this.overrideStockDrops = true;
        }
        if (dropInstance.utilizesGroupIds) {
            this.utilizesGroupIds = true;
        }

        this.customItems.addAll(dropInstance.customItems);
    }

    public @NotNull String getMobOrGroupName() {
        if (this.associatedMob != null) {
            return this.associatedMob.name();
        } else if (this.entityGroup != null) {
            return this.entityGroup.name();
        } else {
            return ""; // this return should never happen
        }
    }

    public @Nullable EntityType getAssociatedMobType(){
        return this.associatedMob;
    }

    public @NotNull String toString() {
        if (this.associatedMob != null) {
            return this.overrideStockDrops ?
                this.associatedMob.name() + " - override" :
                this.associatedMob.name();
        } else if (this.entityGroup != null) {
            return this.overrideStockDrops ?
                this.entityGroup + " - override" :
                this.entityGroup.toString();
        } else {
            return "CustomDropInstance";
        }
    }
}
