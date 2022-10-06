package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.CachedModalList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ModalListParsingInfo {
    public ModalListParsingInfo(final @NotNull ModalListParsingTypes type){
        this.type = type;
    }

    public final ModalListParsingTypes type;
    public String configurationKey;
    public String itemName;
    public boolean supportsGroups;
    public CachedModalList<?> cachedModalList;
    public Map<String, List<String>> groupMapping;
}
