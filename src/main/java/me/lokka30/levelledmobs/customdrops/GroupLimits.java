package me.lokka30.levelledmobs.customdrops;

/**
 * Holds settings used for the group-limits feature
 * of custom drops
 *
 * @author stumper66
 * @since 3.13.0
 */
public class GroupLimits {
    public int capTotal;
    public int capEquipped;
    public int capPerItem;
    public int capSelect;
    public int retries;

    public boolean isEmpty(){
        return capTotal <= 0 &&
                capEquipped <= 0 &&
                capPerItem <= 0 &&
                capSelect <= 0 &&
                retries <= 0;
    }

    public boolean hasCapTotal(){
        return this.capTotal > 0;
    }

    public boolean hasCapEquipped(){
        return this.capEquipped > 0;
    }

    public boolean hasCapPerItem(){
        return this.capPerItem > 0;
    }

    public boolean hasCapSelect(){ return this.capSelect > 0; }

    public boolean hasReachedCapTotal(final int amount){
        return hasCapTotal() && amount >= this.capTotal;
    }

    public boolean hasReachedCapEquipped(final int amount){
        return hasCapEquipped() && amount >= this.capEquipped;
    }

    public boolean hasReachedCapPerItem(final int amount){
        return hasCapPerItem() && amount >= this.capPerItem;
    }

    public boolean hasReachedCapSelect(final int amount){ return hasCapSelect() && amount >= this.capSelect; }

    public String toString(){
        return String.format(
                "capTotal: %s, capEquip: %s, capPerItem: %s, capSelect: %s, retries: %s",
                capTotal, capEquipped, capPerItem, capSelect, retries);
    }
}
