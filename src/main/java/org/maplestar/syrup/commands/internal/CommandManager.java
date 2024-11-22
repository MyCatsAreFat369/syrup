package org.maplestar.syrup.commands.internal;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

public class CommandManager extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private final HashSet<AbstractCommand> commands;

    public CommandManager() {
        this.commands = new HashSet<>();
    }

    public void registerCommand(AbstractCommand command) {
        this.commands.add(command);
        logger.info("Registered command {}", command.name());
    }

    public Collection<SlashCommandData> getCommandData() {
        return commands.stream()
                .map(AbstractCommand::getSlashCommandData)
                .toList();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) return;

        commands.stream()
                .filter(command -> command.name().equals(event.getName()))
                .forEach(command -> command.execute(event));
    }
}
