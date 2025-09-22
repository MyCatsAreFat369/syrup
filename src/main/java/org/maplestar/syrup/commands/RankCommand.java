package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.rank.RankingData;
import org.maplestar.syrup.utils.EmbedMessage;
import org.maplestar.syrup.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The /rank command for displaying the rank of a user.
 */
public class RankCommand extends AbstractCommand {
    private final Logger logger = LoggerFactory.getLogger(RankCommand.class);
    private final LevelDataManager levelDataManager;

    /**
     * Initializes the command.
     *
     * @param levelDataManager the level data manager
     */
    public RankCommand(LevelDataManager levelDataManager) {
        super("rank");

        this.levelDataManager = levelDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "View your rank")
                .setGuildOnly(true)
                .addOption(OptionType.USER, "user", "The user", false);
    }

    /**
     * The /rank command.
     * <p>
     * Sends an image with the user's name, rank, level, XP, and remaining XP until level-up on the current guild.
     * The user's banner or avatar (as a fallback) is used as the background.
     * In case of a database failure, the rank will be "Invalid", and all other values zero.
     * <p>
     * If the image creation fails, the content is instead sent as an embed.
     *
     * @param event the command event
     * @see ImageUtils
     * @see RankingData#zero(User)
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var member = event.getOption("user", event.getMember(), OptionMapping::getAsMember);
        if (member == null) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                            """
                            This user isn't a member of the guild anymore, or they have deleted their account!
                            If you would still like to know their level and xp, please run ``/download leaderboard``
                            and find their User ID in the file in your favorite text editor or Excel! (Usually through
                            Ctrl+F or F3)."""
            )).queue();
            return;
        }

        RankingData rankingData;
        rankingData = levelDataManager.getRankingData(member.getUser(), event.getGuild());

        try {
            var imageBytes = ImageUtils.generateRankImage(member, rankingData);
            event.getHook().editOriginalAttachments(AttachedFile.fromData(imageBytes, member.getUser().getName() + ".png")).queue();
        } catch (Exception exception) {
            logger.error("Couldn't attach rank file", exception);
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    """
                    *Something went wrong while generating your rank image, but here you go:*
                    
                    **%s** is **Rank %s** with **Level %d** (**%,d XP**). %,d more XP is required to level up.""".formatted(
                            member.getEffectiveName(),
                            rankingData.isInvalid() ? "Invalid" : "#" + rankingData.rank(),
                            rankingData.levelData().level(),
                            rankingData.levelData().xp(),
                            rankingData.levelData().remainingXPForLevelup()
                    )
            )).queue();
        }
    }
}
