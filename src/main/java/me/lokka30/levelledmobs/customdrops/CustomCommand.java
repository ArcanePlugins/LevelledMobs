/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * Holds any custom commands as parsed from customdrops.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
public class CustomCommand extends CustomDropBase {

    CustomCommand(@NotNull final CustomDropsDefaults defaults) {
        super(defaults);
        this.rangedEntries = new TreeMap<>();
        this.commands = new LinkedList<>();
        this.runOnDeath = true;
    }

    String commandName;
    @NotNull final public List<String> commands;
    @NotNull final Map<String, String> rangedEntries;
    public boolean runOnSpawn;
    public boolean runOnDeath;
    public int delay;

    public CustomCommand cloneItem() {
        CustomCommand copy = null;
        try {
            copy = (CustomCommand) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
