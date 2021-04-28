package me.lokka30.levelledmobs.misc;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CachedModalList {
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
        switch (listMode) {
            case WHITELIST:
                for (CustomUniversalGroups group : lmEntity.getApplicableGroups()) {
                    if (groups.contains(group)) return true;
                }
                return items.containsKey(lmEntity.getTypeName());
            case BLACKLIST:
                boolean isInGroup = false;
                for (CustomUniversalGroups group : lmEntity.getApplicableGroups()) {
                    if (groups.contains(group)) {
                        isInGroup = true;
                        break;
                    }
                }

                if (items.containsKey(lmEntity.getTypeName())) isInGroup = true;

                return !isInGroup;
            default:
            // mode = all
                return true;
        }
    }
}
