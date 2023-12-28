package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.CachedModalList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Holds various info used for parsing rules.yml
 *
 * @author stumper66
 * @since 3.7.5
 */
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
