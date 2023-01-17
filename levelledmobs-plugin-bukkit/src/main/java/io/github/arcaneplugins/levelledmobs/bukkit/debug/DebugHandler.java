package io.github.arcaneplugins.levelledmobs.bukkit.debug;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public final class DebugHandler {

    private static final EnumSet<DebugCategory> enabledCategories = EnumSet.noneOf(DebugCategory.class);

    public static boolean isCategoryEnabled(final DebugCategory debugCategory) {
        return getEnabledCategories().contains(debugCategory);
    }

    public static void load() {
        final CommentedConfigurationNode parentNode = LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg().getRoot()
            .node("advanced", "debug", "enabled-categories");

        if(parentNode.virtual())
            return;

        try {
            if(parentNode.hasChild("in-list")) {
                final List<String> enabledCategoriesStr = parentNode.node("in-list")
                    .getList(String.class, new LinkedList<>());

                for(final String enabledCategoryStr : enabledCategoriesStr) {
                    try {
                        final DebugCategory debugCategory = DebugCategory.valueOf(
                            enabledCategoryStr.toUpperCase(Locale.ROOT)
                        );
                        getEnabledCategories().add(debugCategory);
                    } catch(IllegalArgumentException ignored) {
                        Log.war("Unknown debug category '" +
                            enabledCategoryStr + "'.", true);
                    }
                }
            } else if(parentNode.hasChild("not-in-list")) {
                final List<String> disabledCategoriesStr = parentNode.node("in-list")
                    .getList(String.class, new LinkedList<>());

                debugCategoryIter:
                for(final DebugCategory debugCategory : DebugCategory.values()) {
                    for(final String disabledCategoryStr : disabledCategoriesStr) {
                        if(debugCategory.name().equalsIgnoreCase(disabledCategoryStr))
                            continue debugCategoryIter;
                    }

                    getEnabledCategories().add(debugCategory);
                }
            }
        } catch(final ConfigurateException ex) {
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    public static Collection<DebugCategory> getEnabledCategories() {
        return enabledCategories;
    }

}
