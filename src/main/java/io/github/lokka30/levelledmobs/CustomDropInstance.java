package io.github.lokka30.levelledmobs;

import org.bukkit.entity.EntityType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CustomDropInstance {
    public CustomDropInstance(final EntityType associatedMob){
        this.associatedMob = associatedMob;
        this.entityGroup = null;
        customItems = new ArrayList<>();
    }

    public CustomDropInstance(final CustomDropsUniversalGroups entityGroup){
        this.associatedMob = null;
        this.entityGroup = entityGroup;
        customItems = new ArrayList<>();
    }

    public boolean getIsGroup(){
        return this.entityGroup != null;
    }

    final public EntityType associatedMob;
    final public CustomDropsUniversalGroups entityGroup;
    @Nonnull
    final public List<CustomItemDrop> customItems;
    public boolean overrideStockDrops;

    @Nonnull
    public String getMobOrGroupName(){
        if (this.associatedMob != null)
            return this.associatedMob.name();
        else if (this.entityGroup != null)
            return this.entityGroup.name();
        else
            return ""; // this return should never happen
    }
}
