package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

/**
 * Holds two int values that are usually used to
 * define a min and max value
 *
 * @author stumper66
 * @since 3.2.0
 */
public class MinAndMax implements Comparable<MinAndMax> {

    public int min;
    public int max;

    public boolean isEmpty() {
        return (min == 0 && max == 0);
    }

    public String toString() {
        return String.format("%s-%s", this.min, this.max);
    }

    @Override
    public int compareTo(@NotNull final MinAndMax o) {
        if (o.min == this.min && o.max == this.max) {
            return 0;
        } else {
            return 1;
        }
    }
}
