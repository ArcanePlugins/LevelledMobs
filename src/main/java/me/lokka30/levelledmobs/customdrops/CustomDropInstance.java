package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Holds a mob or group instance and associates it
 * with a list of custom drop items.
 * This is where the override for a mob / group is set
 *
 * @author stumper66
 */
public class CustomDropInstance {
    public CustomDropInstance(final EntityType associatedMob){
        this.associatedMob = associatedMob;
        this.entityGroup = null;
        this.customItems = new LinkedList<>();
    }

    public CustomDropInstance(final CustomUniversalGroups entityGroup){
        this.associatedMob = null;
        this.entityGroup = entityGroup;
        this.customItems = new LinkedList<>();
    }

    final public EntityType associatedMob;
    final public CustomUniversalGroups entityGroup;
    final public List<CustomDropBase> customItems;
    public Double overallChance;
    public boolean overrideStockDrops;
    public boolean utilizesGroupIds;

    public boolean getIsGroup() {
        return this.entityGroup != null;
    }

    public void combineDrop(CustomDropInstance dropInstance){
        if (dropInstance == null) throw new NullPointerException("dropInstance");

        if (dropInstance.overrideStockDrops) this.overrideStockDrops = true;
        if (dropInstance.utilizesGroupIds) this.utilizesGroupIds = true;

        this.customItems.addAll(dropInstance.customItems);
    }

    @NotNull
    public String getMobOrGroupName() {
        if (this.associatedMob != null)
            return this.associatedMob.name();
        else if (this.entityGroup != null)
            return this.entityGroup.name();
        else
            return ""; // this return should never happen
    }

    @NotNull
    public String toString() {
        if (this.associatedMob != null) {
            return this.overrideStockDrops ?
                    this.associatedMob.name() + " - override" :
                    this.associatedMob.name();
        } else if (this.entityGroup != null) {
            return this.overrideStockDrops ?
                    this.entityGroup + " - override" :
                    this.entityGroup.toString();
        }
        else
            return "CustomDropInstance";
    }
}
