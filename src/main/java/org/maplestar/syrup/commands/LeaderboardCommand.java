package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.utils.EmbedMessage;
import org.maplestar.syrup.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The /leaderboard command for comparing ranks within a guild.
 */
public class LeaderboardCommand extends AbstractCommand {
    private final Logger logger = LoggerFactory.getLogger(LeaderboardCommand.class);
    private final LevelDataManager levelDataManager;
    private final ExecutorService executorService;

    /**
     * Initializes the command
     *
     * @param levelDataManager the level data manager
     */
    public LeaderboardCommand(LevelDataManager levelDataManager) {
        super("leaderboard");

        this.levelDataManager = levelDataManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "View the leaderboard!")
                .setGuildOnly(true)
                .addOption(OptionType.INTEGER, "page", "The page", false);
    }

    /**
     * The /leaderboard command.
     * <p>
     * Displays at most 10 users' ranking information on the requested page (default: 1) for the current guild.
     * If the page exceeds the minimum, it will default to 1.
     * If the page exceeds the maximum, it will default to the maximum page.
     *
     * @param event the command event
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        executorService.submit(() -> {
            int page = event.getOption("page", 1, OptionMapping::getAsInt);
            int totalPages = levelDataManager.getMaxPage(event.getGuild());
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            var guild = event.getGuild();
            var rankedUsers = levelDataManager.getTopUsers(guild, page);
            var userRank = levelDataManager.getRankingData(event.getUser(), guild);
            var member = event.getMember();

            try {
                var imageBytes = ImageUtils.generateLeaderboardImage(rankedUsers, userRank, guild, page, totalPages);
                event.getHook().editOriginalAttachments(AttachedFile.fromData(imageBytes, member.getUser().getName() + ".png")).queue();
            } catch (Exception exception) {
                logger.error("Couldn't attach leaderboard file", exception);
                String desc = String.format(
                        "You currently have **%,d** XP (Level **%d**)" + (userRank.isInvalid() ? "" : " and are in position **#%,d**"),
                        userRank.levelData().xp(),
                        userRank.levelData().level(),
                        userRank.rank());
                event.getHook().editOriginalEmbeds(
                        EmbedMessage.error("*Something went wrong while generating the leaderboard, but here's your personal info:*\n\n" + desc)
                ).queue();
            }
        });
    }
}
