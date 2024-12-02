package org.maplestar.syrup.executors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.utils.EmbedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReminderExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ReminderExecutor.class);
    private final JDA jda;
    private final ReminderDataManager reminderDataManager;

    public ReminderExecutor(JDA jda, ReminderDataManager reminderDataManager) {
        this.jda = jda;
        this.reminderDataManager = reminderDataManager;
    }

    public void init() {
        try {
            jda.awaitReady();
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        var executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            var reminders = reminderDataManager.getSortedReminders();
            var currentTime = LocalDateTime.now();

            while (!reminders.isEmpty() && currentTime.isAfter(reminders.first().time())) {
                var reminder = reminders.removeFirst();
                logger.info("Sending reminder: {}", reminder);

                var channel = jda.getChannelById(MessageChannel.class, reminder.channelID());
                if (channel == null) {
                    logger.info("Channel was deleted, deleting reminder...");
                    reminderDataManager.deleteReminder(reminder);
                    continue;
                }

                var message = reminder.message();
                if (message == null) message = "Ping pong!";

                try {
                    channel.sendMessage("<@" + reminder.userID() + ">")
                            .setEmbeds(EmbedMessage.normalWithTitle("Reminder", message))
                            .queue();
                } catch (InsufficientPermissionException exception) {
                    logger.warn("Insufficient permissions", exception);
                }

                reminderDataManager.deleteReminder(reminder);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
