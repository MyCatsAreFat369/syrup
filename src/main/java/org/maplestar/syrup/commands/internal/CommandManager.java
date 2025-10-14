package org.maplestar.syrup.commands.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

/**
 * Stores and handles commands to be run via Discord.
 */
public class CommandManager extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private final HashSet<AbstractCommand> commands;

    /**
     * Initializes the command manager.
     */
    public CommandManager() {
        this.commands = new HashSet<>();
    }

    /**
     * Registers an {@link AbstractCommand} so it can be run via Discord.
     *
     * @param command the command to be registered
     */
    public void registerCommand(AbstractCommand command) {
        this.commands.add(command);
        logger.info("Registered command /{}", command.name());
    }

    /**
     * Returns a collection of the {@link SlashCommandData} configuration associated
     * with all registered commands, which in turn is registered on Discord via JDA.
     *
     * @return a collection of all command's data
     */
    public Collection<SlashCommandData> getCommandData() {
        return commands.stream()
                .map(AbstractCommand::getSlashCommandData)
                .toList();
    }

    /**
     * Handles the execution of a slash command. Is only called when the command belongs to this bot.
     * It delegates the execution to the commands registered with this class, if possible.
     *
     * @param event a {@link SlashCommandInteractionEvent} representing the command
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String guildName = "DMs";
        if (event.isFromGuild()) guildName = event.getGuild().getName();

        logger.info("Received command {} from {} in {}", event.getCommandString(), event.getUser().getName(), guildName);

        commands.stream()
                .filter(command -> command.name().equals(event.getName()))
                .forEach(command -> command.execute(event));
    }
}
