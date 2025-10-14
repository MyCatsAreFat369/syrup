package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.listener.LevelChangeListener;
import org.maplestar.syrup.listener.event.LevelChangeEvent;
import org.maplestar.syrup.utils.EmbedMessage;

/**
 * The /editrank command for editing a user's level or xp.
 */
public class EditRankCommand extends AbstractCommand {
    private final LevelDataManager levelDataManager;
    private final LevelChangeListener levelChangeListener;

    /**
     * Initializes the command.
     *
     * @param levelDataManager the level data manager
     * @param levelChangeListener the level change listener, to notify of potential level changes
     */
    public EditRankCommand(LevelDataManager levelDataManager, LevelChangeListener levelChangeListener) {
        super("editrank");

        this.levelDataManager = levelDataManager;
        this.levelChangeListener = levelChangeListener;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Edit a user's rank")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, "user", "The user", true)
                .addOptions(new OptionData(OptionType.STRING, "type", "The type", true)
                        .addChoice("XP", "XP")
                        .addChoice("Level", "LEVEL")
                )
                .addOption(OptionType.INTEGER, "value", "The new value", true);
    }

    /**
     * The /editrank command.
     * <p>
     * Can be used by Administrators to manually alter a user's level or XP amount on the current guild.
     * Levelroles will be applied accordingly.
     *
     * @param event the command event
     */
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        var user = event.getOption("user").getAsUser();
        var type = RankCommandType.valueOf(event.getOption("type").getAsString());
        var value = event.getOption("value").getAsInt();

        if (user.isBot()) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("Oops! Bot accounts can't have a rank.")).queue();
            return;
        }

        var oldLevelData = levelDataManager.getLevelData(user, event.getGuild());
        LevelData newLevelData = null;

        if (type == RankCommandType.XP) {
            newLevelData = oldLevelData.setXP(value);
        } else if (type == RankCommandType.LEVEL) {
            newLevelData = oldLevelData.setLevel(value);
        }

        if (newLevelData.level() >= 420) {
            newLevelData = LevelData.MAX;
        }

        var success = levelDataManager.setLevelData(user, event.getGuild(), newLevelData);
        if (!success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to edit the user's rank.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
            return;
        }

        if (newLevelData.level() != oldLevelData.level()) {
            levelChangeListener.onLevelChange(new LevelChangeEvent(event.getGuild(), user, oldLevelData, newLevelData));
        }

        event.getHook().editOriginalEmbeds(EmbedMessage.normal(
                String.format(
                        "Set user %s's %s to %s",
                        user.getAsMention(),
                        type.toString().toLowerCase(),
                        type == RankCommandType.LEVEL && value != newLevelData.level() ? "the maximum of " + LevelData.MAX.level() : value
                )
        )).queue();
    }

    /**
     * Represents the type argument of the /rank edit subcommand.
     */
    private enum RankCommandType {
        LEVEL, XP
    }
}
