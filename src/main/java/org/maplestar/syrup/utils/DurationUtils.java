package org.maplestar.syrup.utils;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Utility class for dealing with {@link Duration}.
 */
public class DurationUtils {
    private static final Pattern durationPattern = Pattern.compile("(\\d+[smhdy])");

    /**
     * Attempts to parse the provided String as a Duration.
     * <p>
     * It is expected to consist of a number followed by either y for year, d for day,
     * h for hour, m for minute or s for second.
     * Multiple ones may be chained together to express more complex durations.
     *
     * @param durationString the human-readable String representing the duration
     * @return the parsed duration
     * @throws IllegalArgumentException if the provided String couldn't be parsed
     */
    public static Duration durationStringToMillis(String durationString) throws IllegalArgumentException {
        var matcher = durationPattern.matcher(durationString.toLowerCase());
        if (!matcher.find()) throw new IllegalArgumentException();

        var duration = Duration.ZERO;
        do {
            var group = matcher.group(1);
            var amount = Integer.parseInt(group.substring(0, group.length() - 1));
            switch (group.toCharArray()[group.length() - 1]) {
                case 's' -> duration = duration.plusSeconds(amount);
                case 'm' -> duration = duration.plusMinutes(amount);
                case 'h' -> duration = duration.plusHours(amount);
                case 'd' -> duration = duration.plusDays(amount);
                case 'y' -> duration = duration.plusDays(365L * amount);
                default -> throw new IllegalArgumentException();
            }
        } while (matcher.find());

        return duration;
    }
}
