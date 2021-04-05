package me.lokka30.levelledmobs.customdrops;

/**
 * @author stumper66
 */
public class CustomDropsDefaults {
    public double chance;
    public int amount;
    public int minLevel;
    public int maxLevel;
    public boolean noMultiplier;
    public boolean noSpawner;
    public String damage;
    public boolean equipped;
    public int customModelData;
    public String groupId;
    public boolean override;

    public CustomDropsDefaults() {
        // these are the defaults of the defaults
        this.chance = 0.2;
        this.amount = 1;
        this.minLevel = -1;
        this.maxLevel = -1;
        this.customModelData = -1;
        this.noMultiplier = false;
        this.noSpawner = false;
        this.equipped = false;
        this.override = false;
    }

    public void setDefaultsFromDropItem(CustomItemDrop drop) {
        this.chance = drop.dropChance;
        this.amount = drop.getAmount();
        this.minLevel = drop.minLevel;
        this.maxLevel = drop.maxLevel;
        this.customModelData = drop.customModelDataId;
        this.noMultiplier = drop.noMultiplier;
        this.noSpawner = drop.noSpawner;
        this.equipped = drop.isEquipped;
    }
}
