package me.lokka30.levelledmobs.customdrops;

public class GroupLimits {
    public int capTotal;
    public int capEquipped;
    public int capPerItem;
    public int retries;

    public boolean isEmpty(){
        return capTotal == 0 &&
                capEquipped == 0 &&
                capPerItem == 0 &&
                retries == 0;
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

    public boolean hasReachedCapTotal(final int amount){
        return hasCapTotal() && amount >= this.capTotal;
    }

    public boolean hasReachedCapEquipped(final int amount){
        return hasCapEquipped() && amount >= this.capEquipped;
    }

    public boolean hasReachedCapPerItem(final int amount){
        return hasCapPerItem() && amount >= this.capPerItem;
    }

    public String toString(){
        return String.format(
                "drop: %s, equip: %s, lmt-amount: %s, retries: %s",
                capTotal, capEquipped, capPerItem, retries);
    }
}
