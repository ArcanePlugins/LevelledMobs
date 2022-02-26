/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.debug;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;

public final class DebugHandler {

    public void load() {
        for (
                String debugCategoryStr : LevelledMobs.getInstance().getFileHandler().getSettingsFile().getData()
                .getOrDefault("debug-categories", new ArrayList<String>())
        ) {
            final DebugCategory debugCategory;
            try {
                debugCategory = DebugCategory.valueOf(debugCategoryStr);
            } catch(IllegalArgumentException ex) {
                Utils.LOGGER.error("An invalid debug category was speciied in settings.yml - '&b" + debugCategoryStr + "&7' - please fix this.");
                continue;
            }

            getEnabledDebugCategories().add(debugCategory);
        }
    }

    private final EnumSet<DebugCategory> enabledDebugCategories = EnumSet.noneOf(DebugCategory.class);
    public @NotNull EnumSet<DebugCategory> getEnabledDebugCategories() { return enabledDebugCategories; }

    /**
     * @param category category that is being checked
     * @return if the category is enabled or not
     * Check if a debug category is enabled or not by the configuration.
     * @author lokka30
     * @since 4.0.0
     */
    public boolean isDebugCategoryEnabled(final DebugCategory category) {
        return getEnabledDebugCategories().contains(category);
    }

    public void sendDebugLog(final DebugCategory category, final String msg) {
        Utils.LOGGER.info("&8[&3Debugging&8 - &3" + category + "&8]: &7" + msg);
    }
}
