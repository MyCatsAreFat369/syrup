package org.maplestar.syrup.commands.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * A slash command to be registered with JDA and run.
 */
public abstract class AbstractCommand {
    protected final String name;

    /**
     * Initialized the command with the provided name.
     *
     * @param name the name of the command, as displayed on Discord
     */
    public AbstractCommand(String name) {
        this.name = name;
    }

    /**
     * the name of the command, as displayed on Discord.
     * Should be the same when configuring the command in {@link AbstractCommand#getSlashCommandData()}.
     *
     * @return the name of this command
     */
    public String name() {
        return name;
    }

    /**
     * Configures the slash command so it can be registered on Discord using JDA.
     * All commands should be configured so they can only be run on guilds.
     * Additionally, considerations about who is allowed to run them should be made.
     *
     * @return the slash command's data
     */
    public abstract SlashCommandData getSlashCommandData();

    /**
     * Executed when this command is run on Discord, with context provided through JDA's {@link SlashCommandInteractionEvent}.
     *
     * @param event the event representing this command execution
     */
    public abstract void execute(SlashCommandInteractionEvent event);
}
