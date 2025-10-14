package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.reminder.Reminder;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.utils.DurationUtils;
import org.maplestar.syrup.utils.EmbedMessage;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * The /remindme command for creating reminders.
 */
public class RemindMeCommand extends AbstractCommand {
    private final ReminderDataManager reminderDataManager;

    /**
     * Initializes the command.
     *
     * @param reminderDataManager the reminder data manager
     */
    public RemindMeCommand(ReminderDataManager reminderDataManager) {
        super("remindme");

        this.reminderDataManager = reminderDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Create a reminder for yourself")
                .addOption(OptionType.STRING, "time", "The time", true)
                .addOption(OptionType.STRING, "message", "A custom message");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var user = event.getUser();
        var timeString = event.getOption("time").getAsString();
        var message = event.getOption("message", null, OptionMapping::getAsString);

        if (reminderDataManager.getUserReminderCount(user) >= 100) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                            Whoops! You've got... 100 pending remindme's!
                            Run ``/remindme-nuke`` to ~~clear~~ nuke all of them.
                            Choose wisely..."""
            )).queue();
            return;
        }

        Duration duration;
        try {
            duration = DurationUtils.durationStringToMillis(timeString);
        } catch (IllegalArgumentException exception) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Not a valid time string! Valid options are: y, w, d, h, m, s.
                    
                    *Some examples of a valid time are "30s" or "5d 12h"*
                    """))
                    .queue();
            return;
        }

        if (event.isFromGuild()) {
            if (!event.getGuildChannel().canTalk(event.getGuild().getSelfMember())) {
                event.getHook().editOriginalEmbeds(EmbedMessage.error("I can't message you in this channel, please fix the permissions!")).queue();
                return;
            }
        }

        var reminder = new Reminder(
                -1,
                user.getIdLong(),
                LocalDateTime.now().plus(duration),
                message,
                event.getChannelIdLong()
        );

        boolean success = reminderDataManager.addReminder(reminder);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Got it! I'll remind you on <t:" + reminder.timeInSeconds() + "> in this channel.")).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to create the reminder.
                    
                    Please contact the bot developer as this is an internal issue."""))
                    .queue();
        }
    }
}
