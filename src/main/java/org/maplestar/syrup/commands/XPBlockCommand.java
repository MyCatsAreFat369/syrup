package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.block.BlockDataManager;

public class XPBlockCommand extends AbstractCommand {
    private final BlockDataManager blockDataManager;

    public XPBlockCommand(BlockDataManager blockDataManager) {
        super("xp");

        this.blockDataManager = blockDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Deals with XP gain in channels")
                .setGuildOnly(true)
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

    private void block(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var channel = event.getOption("channel").getAsChannel();

        boolean isBlocked = blockDataManager.isBlocked(channel, guild);
        if (isBlocked) {
            event.getHook().editOriginal("This channel is already in the blocklist!")
                    .queue();
            return;
        }

        boolean success = blockDataManager.setBlocked(channel.getIdLong(), guild, true);
        if (!success) {
            event.getHook().editOriginal("Oops! Failed to toggle xp-block in this channel. " +
                            "Please contact the bot developer as this is an internal issue.")
                    .queue();
            return;
        }

        event.getHook().editOriginal("This channel had been **added** to the xp-blocklist! " +
                        "Users will no longer gain xp by chatting here.")
                .queue();
    }

    private void unblock(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var channel = event.getOption("channel").getAsChannel();

        boolean isBlocked = blockDataManager.isBlocked(channel, guild);
        if (!isBlocked) {
            event.getHook().editOriginal("This channel is not in the blocklist!").queue();
            return;
        }

        boolean success = blockDataManager.setBlocked(channel.getIdLong(), guild, false);
        if (!success) {
            event.getHook().editOriginal("Oops! Failed to toggle xp-block in this channel. Please contact the bot developer as this is an internal issue.").queue();
            return;
        }

        event.getHook().editOriginal("This channel has been **removed** from the xp-blocklist! Users will now gain xp by chatting here.").queue();
    }

    private void cleanup(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var guildChannels = guild.getChannels().stream()
                .map(GuildChannel::getIdLong)
                .toList();

        blockDataManager.getBlockedChannelIds(guild).stream()
                .filter(blockedChannelID -> !guildChannels.contains(blockedChannelID))
                .forEach(invalidChannelID -> blockDataManager.setBlocked(invalidChannelID, guild, false));

        event.getHook().editOriginal("Channels have been cleaned!!").queue();
    }
}
