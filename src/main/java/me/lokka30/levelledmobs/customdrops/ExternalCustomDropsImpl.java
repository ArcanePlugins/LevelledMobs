package me.lokka30.levelledmobs.customdrops;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class allows 3rd parties to add custom drops directly to LevelledMobs
 *
 * @author stumper66
 * @since 3.7.0
 */
public class ExternalCustomDropsImpl implements ExternalCustomDrops {
    public ExternalCustomDropsImpl(){
        this.customDropsitems = new TreeMap<>();
        this.customDropIDs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    final Map<EntityType, CustomDropInstance> customDropsitems;
    final Map<String, CustomDropInstance> customDropIDs;

    public void addCustomDrop(final @NotNull CustomDropInstance customDropInstance){
        this.customDropsitems.put(customDropInstance.getAssociatedMobType(), customDropInstance);
    }

    public void addCustomDropTable(final @NotNull String dropName, final @NotNull CustomDropInstance customDropInstance){
        this.customDropIDs.put(dropName, customDropInstance);
    }

    public @NotNull Map<EntityType, CustomDropInstance> getCustomDrops(){
        return this.customDropsitems;
    }

    public @NotNull Map<String, CustomDropInstance> getCustomDropTables(){
        return this.customDropIDs;
    }

    public void clearAllExternalCustomDrops(){
        this.customDropsitems.clear();
        this.customDropIDs.clear();
    }
}
