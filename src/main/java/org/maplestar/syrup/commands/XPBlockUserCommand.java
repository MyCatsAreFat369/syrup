package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.xpblock.XPBlockData;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;
import org.maplestar.syrup.utils.EmbedMessage;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The /xp command for excluding channels from the rank system.
 */
public class XPBlockUserCommand extends AbstractCommand {
    private final XPBlockDataManager xpBlockDataManager;

    /**
     * Initializes the command.
     *
     * @param xpBlockDataManager the block data manager
     */
    public XPBlockUserCommand(XPBlockDataManager xpBlockDataManager) {
        super("xp-user");

        this.xpBlockDataManager = xpBlockDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Deals with XP gain for users")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("list", "List all xp-blocked users!")
                                .addOption(OptionType.INTEGER, "page", "The page of the xp-blocked users list!", true),
                        new SubcommandData("block", "Block a user from gaining xp!")
                                .addOption(OptionType.USER, "user", "The user", true),
                        new SubcommandData("unblock", "Unblock a user, allowing them to gain xp!!")
                                .addOption(OptionType.USER, "user", "The user", true)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        switch(event.getSubcommandName()) {
            case "list" -> list(event);
            case "block" -> block(event);
            case "unblock" -> unblock(event);
            default -> throw new IllegalArgumentException();
        }
    }

    /**
     * The /xp-user list subcommand.
     * <p>
     * Provides an overview over all xp-blocked users on the current guild.
     *
     * @param event the command event
     */
    private void list(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        int page = event.getOption("page").getAsInt();

        var embedBuilder = new EmbedBuilder()
                .setAuthor(guild.getName(), null, guild.getIconUrl())
                .setDescription("XP-Blocked users in this server");

        var xpBlockedUsers = xpBlockDataManager.getXPBlocks(guild, page);

        for(var xpBlockData : xpBlockedUsers) {
            Member member = guild.retrieveMemberById(xpBlockData.userID()).complete(); // TODO: lacks asynchronous
            ZoneId zoneId = ZoneId.systemDefault();
            long epoch = xpBlockData.time().atZone(zoneId).toEpochSecond();
            embedBuilder.addField(member.getUser().getName(),
                    "**User ID:** " + member.getId() + "\n" +
                    "**Time:** " + "<t:" + epoch + ">", false);
        }

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    /**
     * The /xp-user block subcommand.
     * <p>
     * Excludes the provided user from gaining xp, if they had not already been excluded.
     *
     * @param event the command event
     */
    private void block(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var member = event.getOption("user").getAsMember();

        if(member == null) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("User does not exist.")).queue();
            return;
        }

        boolean isBlocked = xpBlockDataManager.isBlocked(guild, member);
        if (isBlocked) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This channel is already in the blocklist!")).queue();
            return;
        }

        XPBlockData xpBlockData = new XPBlockData(member.getIdLong(), LocalDateTime.now());
        boolean success = xpBlockDataManager.setBlocked(guild, xpBlockData, true);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                This user has been **added** to the XP blocklist!
                
                They will no longer gain xp by chatting."""))
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to block this user.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
        }
    }

    /**
     * The /xp-user unblock subcommand.
     * <p>
     * Enables a user to once again gain experience in the level system if they had previously been blocked
     *
     * @param event the command event
     */
    private void unblock(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var member = event.getOption("user").getAsMember();

        if(member == null) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("User does not exist.")).queue();
            return;
        }

        boolean isBlocked = xpBlockDataManager.isBlocked(guild, member);
        if (!isBlocked) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This user is not in the XP blocklist!")).queue();
            return;
        }

        XPBlockData xpBlockData = new XPBlockData(member.getIdLong(), LocalDateTime.now());
        boolean success = xpBlockDataManager.setBlocked(guild, xpBlockData, false);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                This user has been **removed** from the XP blocklist!
                
                They will now gain xp by chatting."""))
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to unblock this user.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
        }
    }
}
