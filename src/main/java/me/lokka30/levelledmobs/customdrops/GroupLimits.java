package me.lokka30.levelledmobs.customdrops;

public class GroupLimits {
    public int drop;
    public int equip;
    public int amount;
    public int retries;

    public boolean isEmpty(){
        return drop == 0 &&
                equip == 0 &&
                amount == 0 &&
                retries == 0;
    }

    public String toString(){
        return String.format(
                "drop: %s, equip: %s, lmt-amount: %s, retries: %s",
                drop, equip, amount, retries);
    }
}
