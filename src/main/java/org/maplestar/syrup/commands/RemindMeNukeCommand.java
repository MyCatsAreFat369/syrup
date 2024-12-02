package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.utils.EmbedMessage;

public class RemindMeNukeCommand extends AbstractCommand {
    private final ReminderDataManager reminderDataManager;

    public RemindMeNukeCommand(ReminderDataManager reminderDataManager) {
        super("remindme-nuke");

        this.reminderDataManager = reminderDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Nuke all your reminders... Very scary!");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        var user = event.getUser();

        var success = reminderDataManager.nukeReminders(user);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Nuked!")).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    "Uh... there was an error... Please go to Syrup's bio and contact one of the bot devs!"
            )).queue();
        }
    }
}
