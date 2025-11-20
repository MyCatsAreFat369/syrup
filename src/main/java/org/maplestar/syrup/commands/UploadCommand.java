package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The /download command for downloading guild data.
 */
public class UploadCommand extends AbstractCommand
{
    private final Logger logger = LoggerFactory.getLogger(UploadCommand.class);
    private final LevelDataManager levelDataManager;

    /**
     * Initializes the command.
     *
     * @param levelDataManager the level data manager
     */
    public UploadCommand(LevelDataManager levelDataManager) {
        super("upload");

        this.levelDataManager = levelDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Upload previous leaderboard data to this server!")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("leaderboard", "Download this server's leaderboard as a CSV file!")
                                .addOption(OptionType.ATTACHMENT, "file", "Text file in csv format")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
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
    public void leaderboard(SlashCommandInteractionEvent event, Guild guild)
    {
        var attachment = event.getOption("file", null, OptionMapping::getAsAttachment);
        if(attachment == null)
        {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("The file you provided was null for some reason..."))
                    .queue();
            return;
        }
        if(attachment.getFileExtension() == null)
        {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("The file you provided has no file extension!"))
                    .queue();
            return;
        }
        if(!attachment.getFileExtension().equals("txt"))
        {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This file isn't a text file!"))
                    .queue();
            return;
        }


        try
        {
            URI uri = ClassLoader.getSystemResource("temp").toURI();
            String mainPath = Paths.get(uri).toString();
            Path source = Paths.get(mainPath);
            attachment.getProxy().downloadToFile(new File(mainPath + "/" + attachment.getFileName()))
                    .thenAccept(file ->
                    {
                        var rankingDataList = LeaderboardDataToCSVUtils.createDataFromCSVFile(file);
                        while(!rankingDataList.isEmpty())
                        {
                            var rankingData = rankingDataList.removeFirst();
                            var user = event.getJDA().retrieveUserById(rankingData.userID()).complete();
                            if(user == null) continue;
                            if(rankingData.levelData().xp() == 0) continue;
                            levelDataManager.setLevelData(user, event.getGuild(), rankingData.levelData());
                        }
                        logger.info("Saved file to path " + file.getAbsolutePath());
                        event.getChannel().sendMessageEmbeds(EmbedMessage.normal("Uploaded the data!"))
                                .queue();
                    });

            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Gotcha! I'll upload this data into the server, just give me a moment!"))
                    .queue();
        } catch(URISyntaxException exception)
        {
            logger.error("Couldn't upload file", exception);
            event.getHook().editOriginalEmbeds(EmbedMessage.error("Sorry, I came across an issue."))
                    .queue();
        }
    }
}
