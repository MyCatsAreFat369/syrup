/*

package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.maplestar.syrup.commands.internal.AbstractCommand;

public class LogSettingsCommand extends AbstractCommand {
    public LogSettingsCommand() {
        super("logsettings");
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Manage the various settings of this server.")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("list", "List the various settings of this server")
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("set", "Configure the values of various settings")
                                .addSubcommands(
                                        new SubcommandData("log_channel", "Channel where a variety of events and leaderboard backups are logged")
                                                .addOption(OptionType.CHANNEL, "channel", "The channel where various things are logged", true)
                                ),
                        new SubcommandGroupData("enable", "Choose which logging to enable/disable")
                                .addSubcommands(
                                        new SubcommandData("leaderboard_backups", "Daily leaderboard backups are done for this server if enabled.")
                                                .addOption(OptionType.BOOLEAN, "value", "Whether or not to enable these daily backups", true),
                                        new SubcommandData("levelrole_logs", "Configure various options for levelrole logging")
                                                .addOption(OptionType.BOOLEAN, "levelrole_added", "Log whenever a user is given a levelrole", false)
                                                .addOption(OptionType.BOOLEAN, "levelrole_removed", "Log whenever a user's levelrole is removed", false)
                                )
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var subCommandGroup = event.getSubcommandGroup();
        if(subCommandGroup == null) {
            switch(event.getSubcommandName()) {
                case "list" -> list(event);
                case null, default -> throw new IllegalArgumentException();
            }
        } else if(subCommandGroup.equals("set")) {
            switch(event.getSubcommandName()) {
                case "log_channel" -> set_logChannel(event);
                case null, default -> throw new IllegalArgumentException();
            }
        } else if(subCommandGroup.equals("enable")) {
            switch(event.getSubcommandName()) {
                case "leaderboard_backups" -> enable_leaderboardBackups(event);
                case "levelrole_logs" -> enable_levelroleLogs(event);
                case null, default -> throw new IllegalArgumentException();
            }
        }
    }

    private void list(SlashCommandInteractionEvent event) {
        // TODO: implement
    }

    private void set_logChannel(SlashCommandInteractionEvent event) {
        // TODO: implement
    }

    private void enable_leaderboardBackups(SlashCommandInteractionEvent event) {
        // TODO: implement
    }

    private void enable_levelroleLogs(SlashCommandInteractionEvent event) {
        // TODO: implement
    }
}


 */