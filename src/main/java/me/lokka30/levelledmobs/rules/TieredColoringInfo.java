/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the values parsed from rules.yml used with the tiered placeholder for nametags
 *
 * @author stumper66
 * @since 3.0.0
 */
public class TieredColoringInfo implements Cloneable {

    public int minLevel;
    public int maxLevel;
    public String text;
    boolean isDefault;

    private TieredColoringInfo() {
    }

    @NotNull
    static TieredColoringInfo createDefault(final @NotNull String value) {
        final TieredColoringInfo coloringInfo = new TieredColoringInfo();
        coloringInfo.isDefault = true;
        coloringInfo.text = value;

        return coloringInfo;
    }

    @Nullable
    static TieredColoringInfo createFromString(final @NotNull String key,
        final @NotNull String value) {
        final String[] numbers = key.split("-");
        if (numbers.length != 2) {
            Utils.logger.warning("Invalid tiered coloring key: " + key);
            return null;
        }

        final TieredColoringInfo coloringInfo = new TieredColoringInfo();

        for (int i = 0; i < 2; i++) {
            final String num = numbers[i].trim();
            if (!Utils.isInteger(num)) {
                Utils.logger.warning("Invalid number in tiered coloring key: " + key);
                return null;
            }

            if (i == 0) {
                coloringInfo.minLevel = Integer.parseInt(num);
            } else {
                coloringInfo.maxLevel = Integer.parseInt(num);
            }
        }

        coloringInfo.text = value;

        return coloringInfo;
    }

    public String toString() {
        if (isDefault) {
            return this.text + "default&r";
        } else {
            return String.format("%s%s-%s&r", this.text, this.minLevel, this.maxLevel);
        }
    }

    public TieredColoringInfo cloneItem() {
        TieredColoringInfo copy = null;
        try {
            copy = (TieredColoringInfo) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
