package io.github.arcaneplugins.levelledmobs.bukkit.util.math;

import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

public final class RangedInt implements Comparable<RangedInt> {

    private final Integer min;
    private final Integer max;

    public RangedInt(
        final Integer min,
        final Integer max
    ) {
        this.min = min;
        this.max = max;
    }

    public RangedInt(
        final Integer num
    ) {
        this.min = num;
        this.max = num;
    }

    public RangedInt(
        final String string
    ) {

        // split string into two parts: `1-2`.
        final String[] split = string
            .replace(" ", "") // sometimes people use spaces so factor that in
            .split("-");                 // finally we do the split :)

        if(split.length == 1) {
            final int number = Integer.parseInt(split[0]);
            this.min = number;
            this.max = number;
        } else if(split.length == 2) {
            this.min = Integer.valueOf(split[0]);
            this.max = Integer.valueOf(split[1]);
        } else {
            throw new IllegalArgumentException(
                "Invalid ranged int '%s': expected 1 or 2 arguments but received %s"
                    .formatted(string, split.length)
            );
        }
    }

    @NotNull
    public Integer choose() {
        return ThreadLocalRandom.current().nextInt(getMin(), getMax() + 1);
    }

    public boolean contains(int integer) {
        return getMin() <= integer && getMax() >= integer;
    }


    @NotNull
    public Integer getMin() {
        return min;
    }

    @NotNull
    public Integer getMax() {
        return max;
    }

    @Override
    public int compareTo(@NotNull RangedInt o) {
        return Integer.compare(getMax(), o.getMax());
    }
}
