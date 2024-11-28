package org.maplestar.syrup.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EmbedMessage {
    public static MessageEmbed normal(String text) {
        return new EmbedBuilder()
                .setColor(EmbedColors.primary())
                .setDescription(text)
                .build();
    }

    public static MessageEmbed error(String text) {
        return new EmbedBuilder()
                .setColor(EmbedColors.error())
                .setDescription(text)
                .build();
    }
}
