package org.maplestar.syrup.commands.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public abstract class AbstractCommand {
    protected final String name;

    public AbstractCommand(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract SlashCommandData getSlashCommandData();

    public abstract void execute(SlashCommandInteractionEvent event);
}
