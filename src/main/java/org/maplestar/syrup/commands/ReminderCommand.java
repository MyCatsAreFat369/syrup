package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.utils.EmbedColors;
import org.maplestar.syrup.utils.EmbedMessage;

/**
 * The /remindme-nuke command for deleting all reminders.
 */
public class ReminderCommand extends AbstractCommand {
    private final ReminderDataManager reminderDataManager;

    /**
     * Initializes the command.
     *
     * @param reminderDataManager the reminder data manager
     */
    public ReminderCommand(ReminderDataManager reminderDataManager) {
        super("reminder");

        this.reminderDataManager = reminderDataManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Do various things with your reminders!")
                .addSubcommands(
                        new SubcommandData("nuke", "Nuke all your reminders... Very scary!"),
                        new SubcommandData("remove", "Good riddance...")
                                .addOption(OptionType.INTEGER, "id", "The ID of the reminder you wanna get rid of.", true),
                        new SubcommandData("list", "List all the active reminders you've put. Syncs across all servers!")
                                .addOption(OptionType.INTEGER, "page", "The page of the reminders you wanna look at.", false)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        switch (event.getSubcommandName()) {
            case "list" -> list(event);
            case "remove" -> remove(event);
            case "nuke" -> nuke(event);
            case null, default -> throw new IllegalArgumentException();
        }
    }

    private void list(SlashCommandInteractionEvent event) {
        var user = event.getUser();
        var page = event.getOption("page", 1, OptionMapping::getAsInt);
        var reminders = reminderDataManager.getPaginatedRemindersOfUser(user, page);

        int reminderCount = reminderDataManager.getUserReminderCount(user);
        int maxPage = (int) Math.ceil(reminderCount / 5.0);
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(EmbedColors.primary())
                .setTitle(user.getName() + "'s reminders")
                .setFooter("Page " + page + " / " + maxPage + "   |   Total reminders: " + reminderCount);

        for (var reminder : reminders) {
            String message = reminder.message();
            if (message == null) message = "Ping pong!";
            if (message.length() > 500) message = message.substring(0, 500) + "...";

            embedBuilder.addField(
                    "Reminder " + reminder.id(),
                    "Time Remaining: <t:" + reminder.timeInSeconds() + ":R>\n" +
                    "Message: " + message + "\n" +
                    "Channel: <#" + reminder.channelID() + ">",
                    false
            );
        }

        var embedMessage = embedBuilder.build();
        event.getHook().editOriginalEmbeds(embedMessage).queue();
    }

    private void remove(SlashCommandInteractionEvent event) {
        var user = event.getUser();
        var id = event.getOption("id").getAsInt();
        var reminderOptional = reminderDataManager.getReminderByID(id);

        if (reminderOptional.isEmpty()) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This reminder doesn't exist! Check /reminder list")).queue();
            return;
        }

        var reminder = reminderOptional.get();
        if (reminder.userID() != user.getIdLong()) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("This isn't your reminder! Check /reminder list")).queue();
            return;
        }
        
        var success = reminderDataManager.deleteReminder(reminder);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Deleted reminder " + id + "!")).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    "Uh... there was an error... Please go to Syrup's bio and contact one of the bot devs!"
            )).queue();
        }
    }

    private void nuke(SlashCommandInteractionEvent event) {
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
