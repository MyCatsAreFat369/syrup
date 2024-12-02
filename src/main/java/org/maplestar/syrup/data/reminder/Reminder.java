package org.maplestar.syrup.data.reminder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record Reminder(int id, long userID, LocalDateTime time, @Nullable String message, long channelID) implements Comparable {
    @Override
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof Reminder reminder)) return 0;

        return time.compareTo(reminder.time);
    }

    public long timeInMilli() {
        return this.time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli();
    }

    public long timeInSeconds() {
        return timeInMilli() / 1000;
    }

    public Reminder withID(int id) {
        return new Reminder(id, userID, time, message, channelID);
    }
}
