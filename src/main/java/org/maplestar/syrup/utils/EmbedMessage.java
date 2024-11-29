package org.maplestar.syrup.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * Utility class which provides an easy way to build embeds from Strings.
 */
public class EmbedMessage {
    /**
     * Creates an embed to be used for messages that don't represent an error.
     *
     * @param text the content of the embed
     * @return the embed
     */
    public static MessageEmbed normal(String text) {
        return new EmbedBuilder()
                .setColor(EmbedColors.primary())
                .setDescription(text)
                .build();
    }

    /**
     * Creates an embed to be used for messages that represent an error.
     *
     * @param text the content of the embed
     * @return the embed
     */
    public static MessageEmbed error(String text) {
        return new EmbedBuilder()
                .setColor(EmbedColors.error())
                .setDescription(text)
                .build();
    }
}
