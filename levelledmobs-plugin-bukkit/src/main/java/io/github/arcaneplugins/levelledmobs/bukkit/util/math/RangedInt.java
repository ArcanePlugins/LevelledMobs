package io.github.arcaneplugins.levelledmobs.bukkit.util.math;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
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
        @Nonnull String str
    ) {
        Objects.requireNonNull(str, "str");

        /*
        note: this parser has been tested with the following inputs:

        10  -10     10-20       -10-20     -10--20     10--20       10 -20

        10 --20     10 - -20    -10 -20    -10- -20
         */

        // remove whitespace to simplify parsing
        str = str.replace(" ", "");

        // we convert it to a char array to efficiently iterate through it
        final char[] chars = str.toCharArray();

        if(chars.length == 0)
            throw new IllegalArgumentException("Input string can't be empty");

        // states which side we are currently parsing. left=true; right=false
        // note that in ranged ints, the right side value is optional.
        // also note that the min/max value can be on either side.
        boolean isLeft = true;

        // information for parsing the left side
        final int leftStart = 0; // index which the left side starts. constant
        int leftEnd = chars.length - 1; // index which the left side ends

        int rightStart = -1; // index which the right side starts. init'd with an invalid val
        final int rightEnd = leftEnd; // index which the right side ends. constant

        // iterate through each character in the input. divide it between left and right side
        for(int i = 0; i < chars.length; i++) {
            // character at current index
            final char c = chars[i];

            // we're looking for the range splitter here. if it doesn't exist, the ranged int is
            // just parsed on the left side and ignoring the non-existent right side.
            // ranged ints look like this: -1 - -50, where -1 is the left side and -50 is the right.
            // notice how the hyphen character is used to separate the range and this character is
            // also used for the negative sign so we can't just split the string into 2, unless
            // the number is unsigned, which we don't care about in Java most of the time
            if(isLeft && i != 0 && c == '-') {
                leftEnd = i - 1;
                if(i == chars.length - 1) {
                    throw new IllegalArgumentException("Missing right side of RangedInt declaration");
                } else {
                    rightStart = i + 1;
                    isLeft = false;
                }
            }
        }

        // parse the left side
        // note: substring end is exclusive, so we're adding 1
        int left = Integer.parseInt(str.substring(leftStart, leftEnd + 1));

        if(isLeft) {
            // looks like there is no right side, so we'll just set min and max to the left value
            this.min = left;
            this.max = left;
        } else {
            // parse the right side
            // note: substring end is exclusive, so we're adding 1
            int right = Integer.parseInt(str.substring(rightStart, rightEnd + 1));

            // remember that the left side is not always min, and the right side is not always max,
            // so we'll have to feed both through Math.min and Math.max to find out which is which.
            this.min = Math.min(left, right);
            this.max = Math.max(left, right);
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
