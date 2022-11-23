package io.github.arcaneplugins.levelledmobs.bukkit.util.math;

import java.util.function.Function;
import javax.annotation.Nonnull;

public class TimeUtils {

    private TimeUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    public static long parseTimeToTicks(
        final @Nonnull Object o
    ) {
        if(o instanceof final String s) {
            try {
                return Long.parseLong(s);
            } catch(NumberFormatException ignored) {}

            final Function<Integer, Long> parser = (suffixLen) ->
                Long.parseLong(s.substring(0, s.length() - suffixLen));

            if(s.endsWith("t")) {
                return parser.apply(1);
            } else if(s.endsWith("s")) {
                return parser.apply(1) * 20;
            } else if(s.endsWith("ms")) {
                return (long) (parser.apply(2) * 0.002d);
            } else if(s.endsWith("m")) {
                return parser.apply(1) * 1_200;
            } else if(s.endsWith("h")) {
                return parser.apply(1) * 72_000;
            } else if(s.endsWith("d")) {
                return parser.apply(1) * 1_728_000;
            } else {
                throw new IllegalArgumentException("Invalid time unit '%s'."
                    .formatted(s.substring(s.length() - 1)));
            }
        } else if(o instanceof final Number n) {
            return (long) n;
        } else {
            throw new IllegalArgumentException("Unable to parse type " +
                o.getClass().getSimpleName());
        }
    }

}
