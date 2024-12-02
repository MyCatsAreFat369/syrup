package org.maplestar.syrup.data.reminder;

import net.dv8tion.jda.api.entities.User;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

public class ReminderDataManager {
    private final Logger logger = LoggerFactory.getLogger(ReminderDataManager.class);
    private final DatabaseManager databaseManager;
    private final SortedSet<Reminder> reminderCache = new TreeSet<>();

    public ReminderDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        loadReminders();
    }

    private void loadReminders() {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM Reminders")) {
                var resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    var reminder = new Reminder(
                            resultSet.getInt("id"),
                            resultSet.getLong("user_id"),
                            resultSet.getTimestamp("time").toLocalDateTime(),
                            resultSet.getString("message"),
                            resultSet.getLong("channel_id")
                    );

                    reminderCache.add(reminder);
                }
            }
        } catch (SQLException exception) {
            logger.error("Failed to load reminders", exception);
        }
    }

    public int getUserReminderCount(User user) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT COUNT(*) AS count FROM Reminders WHERE user_id = ?")) {
                statement.setLong(1, user.getIdLong());

                var resultSet = statement.executeQuery();
                resultSet.next();
                return resultSet.getInt("count");
            }
        } catch (SQLException exception) {
            logger.error("Couldn't check user reminder count", exception);
            return 0;
        }
    }

    public SortedSet<Reminder> getSortedReminders() {
        return reminderCache;
    }

    public boolean addReminder(Reminder reminder) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO Reminders (user_id, time, message, channel_id) VALUES (?, ?, ?, ?) RETURNING id")) {
                statement.setLong(1, reminder.userID());
                statement.setTimestamp(2, new Timestamp(reminder.time().toInstant(OffsetDateTime.now().getOffset()).toEpochMilli()));
                statement.setString(3, reminder.message());
                statement.setLong(4, reminder.channelID());

                var resultSet = statement.executeQuery();
                if (!resultSet.next()) return false;

                return reminderCache.add(reminder.withID(resultSet.getInt("id")));
            }
        } catch (SQLException exception) {
            logger.error("Couldn't add reminder {} to database", reminder, exception);
            return false;
        }
    }

    public void deleteReminder(Reminder reminder) {
        reminderCache.remove(reminder);

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("DELETE FROM Reminders WHERE id = ?")) {
                statement.setInt(1, reminder.id());
            }
        } catch (SQLException exception) {
            logger.error("Couldn't delete reminder {}", reminder, exception);
        }
    }

    public boolean nukeReminders(User user) {
        reminderCache.removeIf(reminder -> reminder.userID() == user.getIdLong());

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("DELETE FROM Reminders WHERE user_id = ?")) {
                statement.setLong(1, user.getIdLong());
                statement.execute();
                return true;
            }
        } catch (SQLException exception) {
            logger.error("Couldn't delete reminders for user {}", user.getName(), exception);
            return false;
        }
    }
}
