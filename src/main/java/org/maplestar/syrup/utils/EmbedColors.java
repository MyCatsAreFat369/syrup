package org.maplestar.syrup.utils;

import java.awt.*;

/**
 * Utility class with color constants for Discord embeds.
 */
public class EmbedColors {
    private static final Color primaryColor = new Color(167, 230, 112);

    /**
     * The primary color to be used in all Discord embeds.
     *
     * @return the primary color
     */
    public static Color primary() {
        return primaryColor;
    }

    public static Color error() {
        return Color.RED;
    }
}
