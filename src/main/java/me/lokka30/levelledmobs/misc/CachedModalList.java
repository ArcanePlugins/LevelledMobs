package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CachedModalList implements Cloneable {
    public CachedModalList(){
        this.items = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.groups = new LinkedList<>();
        this.listMode = ModalListMode.ALL;
    }

    public ModalListMode listMode;
    @NotNull
    public Map<String, Object> items;
    @NotNull
    public List<CustomUniversalGroups> groups;

    public boolean isEnabledInList(final String item) {
        switch (listMode) {
            case ALL:
                return true;
            case WHITELIST:
                return items.containsKey(item);
            case BLACKLIST:
                return !items.containsKey(item);
        }

        return false;
    }

    public boolean isLivingEntityInList(final LivingEntityWrapper lmEntity) {
        return isLivingEntityInList(lmEntity, false);
    }

    public boolean isLivingEntityInList(final LivingEntityWrapper lmEntity, final boolean checkBabyMobs) {
        switch (listMode) {
            case WHITELIST:
                final String checkName = checkBabyMobs ?
                        lmEntity.getNameIfBaby() :
                        lmEntity.getTypeName();

                for (CustomUniversalGroups group : lmEntity.getApplicableGroups()) {
                    if (groups.contains(group)) return true;
                }
                return items.containsKey(checkName);
            case BLACKLIST:
                boolean isInGroup = false;
                for (CustomUniversalGroups group : lmEntity.getApplicableGroups()) {
                    if (groups.contains(group)) {
                        isInGroup = true;
                        break;
                    }
                }

                // for denies we'll check for both baby and adult variants regardless of baby-mobs-inherit-adult-setting
                if (items.containsKey(lmEntity.getTypeName()) || items.containsKey(lmEntity.getNameIfBaby()))
                    isInGroup = true;

                return !isInGroup;
            default:
            // mode = all
                return true;
        }
    }

    public String toString(){
        if (this.groups.isEmpty())
            return this.listMode + ", " + this.items.keySet();
        else if (!this.items.isEmpty())
            return this.listMode + ", " + this.groups + ", " + this.items.keySet();
        else
            return this.listMode + ", " + this.groups;
    }

    public CachedModalList cloneItem() {
        CachedModalList copy = null;
        try {
            copy = (CachedModalList) super.clone();
            //copy.items = this.items;
            copy.items = this.items;
            copy.groups = this.groups;
        } catch (Exception ignored) {
        }

        return copy;
    }
}
