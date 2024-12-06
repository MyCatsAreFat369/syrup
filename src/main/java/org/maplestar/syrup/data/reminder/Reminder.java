package org.maplestar.syrup.data.reminder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Represents a reminder that has been created by a user.
 *
 * @param id the reminder's ID. May be set to any number when initializing this object
 * @param userID the user ID
 * @param time the date and time this reminder is due at
 * @param message the message of the reminder, may be null
 * @param channelID the channel this reminder was created in
 * @see org.maplestar.syrup.commands.RemindMeCommand
 * @see org.maplestar.syrup.executors.ReminderExecutor
 */
public record Reminder(int id, long userID, LocalDateTime time, @Nullable String message, long channelID) implements Comparable<Reminder> {
    /**
     * Converts this reminder's end time to epoch seconds.
     *
     * @return the time in epoch seconds
     */
    public long timeInSeconds() {
        return time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli() / 1000;
    }

    /**
     * Creates a copy of this reminder with the provided ID.
     *
     * @param id the new ID
     * @return the new reminder
     */
    public Reminder withID(int id) {
        return new Reminder(id, userID, time, message, channelID);
    }

    @Override
    public int compareTo(@NotNull Reminder reminder) {
        return time.compareTo(reminder.time);
    }
}
