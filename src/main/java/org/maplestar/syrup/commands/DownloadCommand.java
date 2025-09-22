package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.utils.EmbedMessage;
import org.maplestar.syrup.utils.LeaderboardDataToCSVUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The /download command for downloading guild data.
 */
public class DownloadCommand extends AbstractCommand {
    private final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);
    private final LevelDataManager levelDataManager;

    /**
     * Initializes the command.
     *
     * @param levelDataManager the level data manager
     */
    public DownloadCommand(LevelDataManager levelDataManager) {
        super("download");

        this.levelDataManager = levelDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Download this server's data!")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("leaderboard", "Download this server's leaderboard as a CSV file!")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var guild = event.getGuild();
        if (guild == null) return;
        if (event.getSubcommandName() == null) return;

        switch (event.getSubcommandName()) {
            case "leaderboard" -> leaderboard(event, guild);
        }
    }

    /**
     * The /download leaderboard subcommand. Allows the download of all level data as a CSV file.
     * @param event the command event
     * @param guild the guild the command was ran in
     */
    public void leaderboard(SlashCommandInteractionEvent event, Guild guild) {
        var data = levelDataManager.getEntireLeaderboard(guild);
        var csvData = LeaderboardDataToCSVUtils.createCSVFileFromData(data);
        if (csvData.length == 0) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("There is no data for this guild")).queue();
            return;
        }

        try {
            event.getHook().editOriginalAttachments(AttachedFile.fromData(csvData, "leaderboardData.txt"))
                    .queue();
        } catch (Exception exception) {
            logger.error("Couldn't attach leaderboard data file", exception);
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    """
                            Something went wrong while generating the server's leaderboard data. Please
                            contact the developers of Syrup as this is a fatal bug. We'll do anything in
                            our power to fix the bug, as well as provide you with the server's leaderboard
                            data!"""
            )).queue();
        }
    }
}
