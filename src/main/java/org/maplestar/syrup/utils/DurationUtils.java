package org.maplestar.syrup.utils;

import java.time.Duration;
import java.util.regex.Pattern;

public class DurationUtils {
    private static final Pattern durationPattern = Pattern.compile("(\\d+[smhdy])");

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
            };
        } while (matcher.find());

        return duration;
    }
}
