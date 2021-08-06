/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

/**
 * Holds any custom commands as parsed from customdrops.yml
 *
 * @author stumper66
 */
public class CustomCommand extends CustomDropBase {

    public CustomCommand(@NotNull final CustomDropsDefaults defaults){
        super(defaults);
        this.rangedEntries = new TreeMap<>();
    }

    public String commandName;
    public String command;
    @NotNull
    public Map<String, String> rangedEntries;

    public CustomCommand cloneItem() {
        CustomCommand copy = null;
        try {
            copy = (CustomCommand) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
