package org.maplestar.syrup.utils;

import java.awt.*;

/**
 * Utility class with color constants for Discord embeds.
 */
public class EmbedColors {
    private static final Color primaryColor = new Color(167, 230, 112);

    /**
     * The primary color to be used in all normal Discord embeds.
     *
     * @return the primary color
     */
    public static Color primary() {
        return primaryColor;
    }

    /**
     * The error color to be used to indicate a problem.
     *
     * @return the error color
     */
    public static Color error() {
        return Color.RED;
    }
}
