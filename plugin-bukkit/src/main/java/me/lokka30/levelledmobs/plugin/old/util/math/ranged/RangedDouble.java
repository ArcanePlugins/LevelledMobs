/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.util.math.ranged;

import java.util.concurrent.ThreadLocalRandom;

public record RangedDouble(double min, double max) implements RangedValue<Double> {

    public static RangedDouble parse(String val) {
        String[] split = val.split("-");

        if(split.length == 1) {
            // not ranged, only a single val is specified
            final double d = Double.parseDouble(split[0]);
            return new RangedDouble(d, d);
        } else if(split.length == 2) {
            // ranged, as two vals are specified
            return new RangedDouble(
                Double.parseDouble(split[0]),
                Double.parseDouble(split[1])
            );
        } else {
            throw new IllegalArgumentException(
                "Incorrect RangedInt formatting for value '" + val + "'.");
        }
    }

    @Override
    public Double generateRandom() {
        return ThreadLocalRandom.current().nextDouble(min, max + 1);
    }

}
