package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.block.BlockDataManager;
import org.maplestar.syrup.utils.EmbedMessage;

/**
 * The /xp-channel command for excluding channels from the rank system.
 */
public class XPBlockChannelCommand extends AbstractCommand {
    private final BlockDataManager blockDataManager;

    /**
     * Initializes the command.
     *
     * @param blockDataManager the block data manager
     */
    public XPBlockChannelCommand(BlockDataManager blockDataManager) {
        super("xp-channel");

        this.blockDataManager = blockDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Deals with XP gain in channels")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("list", "List all channels with xp-block on!"),
                        new SubcommandData("block", "Block a channel from rewarding xp!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel", true),
                        new SubcommandData("unblock", "Unblock a channel, allowing the rewarding of xp!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel", true),
                        new SubcommandData("cleanup", "Clean up any deleted channels from the xp-blocklist")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        switch(event.getSubcommandName()) {
            case "list" -> list(event);
            case "block" -> block(event);
            case "unblock" -> unblock(event);
            case "cleanup" -> cleanup(event);
            case null, default -> throw new IllegalArgumentException();
        }
    }

    /**
     * The /xp-channel list subcommand.
     * <p>
     * Provides an overview over all blocked channels on the current guild.
     *
     * @param event the command event
     */
    private void list(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();

        var embedBuilder = new EmbedBuilder()
                .setAuthor(guild.getName(), null, guild.getIconUrl())
                .setDescription("XP-Blocked channels in this server");

        var blockedChannels = blockDataManager.getBlockedChannelIds(guild);

        for (int i = 0; i < blockedChannels.size(); i++) {
            var channelID = blockedChannels.get(i);
            embedBuilder.addField("Channel " + (i + 1), "<#" + channelID + ">", true);
        }

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    /**
     * The /xp-channel block subcommand.
     * <p>
     * Excludes the provided channel from the ranking system, if it hasn't been excluded already.
     *
     * @param event the command event
     */
    private void block(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var channel = event.getOption("channel").getAsChannel();

        boolean isBlocked = blockDataManager.isBlocked(channel, guild);
        if (isBlocked) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This channel is already in the blocklist!")).queue();
            return;
        }

        boolean success = blockDataManager.setBlocked(channel.getIdLong(), guild, true);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                This channel had been **added** to the xp-blocklist!
                
                Users will no longer gain xp by chatting here."""))
                    .queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to toggle xp-block in this channel.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
        }
    }

    /**
     * The /xp-channel unblock subcommand.
     * <p>
     * Enables users to once again gain experience in the level system in the provided channel if it has previously been blocked.
     *
     * @param event the command event
     */
    private void unblock(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var channel = event.getOption("channel").getAsChannel();

        boolean isBlocked = blockDataManager.isBlocked(channel, guild);
        if (!isBlocked) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This channel is not in the blocklist!")).queue();
            return;
        }

        boolean success = blockDataManager.setBlocked(channel.getIdLong(), guild, false);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                This channel has been **removed** from the xp-blocklist!\s
                
                Users will now gain xp by chatting here."""))
                    .queue();

        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to toggle xp-block in this channel.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
        }
    }

    /**
     * The /xp-channel cleanup subcommand
     * <p>
     * Removes all blocked channels which have since been deleted from the Discord guild from the database.
     *
     * @param event the command event
     */
    private void cleanup(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var guildChannels = guild.getChannels().stream()
                .map(GuildChannel::getIdLong)
                .toList();

        blockDataManager.getBlockedChannelIds(guild).stream()
                .filter(blockedChannelID -> !guildChannels.contains(blockedChannelID))
                .forEach(invalidChannelID -> blockDataManager.setBlocked(invalidChannelID, guild, false));

        event.getHook().editOriginalEmbeds(EmbedMessage.normal("Channels have been cleaned!!")).queue();
    }
}
