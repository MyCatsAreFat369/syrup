package org.maplestar.syrup.data.xpblock;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to the channels for each individual Discord guild where XP can't be gained.
 */
public class XPBlockDataManager {
    private final Logger logger = LoggerFactory.getLogger(XPBlockDataManager.class);
    private final DatabaseManager databaseManager;

    /**
     * Initializes the class.
     *
     * @param databaseManager the database manager for database access
     */
    public XPBlockDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Checks whether a channel in a specific guild has been blocklisted from the level system.
     *
     * @param guild the guild
     * @param member the member
     * @return false if the channel has not been blocklisted or on database failure, otherwise true
     */
    public boolean isBlocked(Guild guild, Member member) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM BlockedUsers WHERE guild_id = ? AND user_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, member.getIdLong());
                return statement.executeQuery().next();
            }
        } catch (SQLException exception) {
            logger.error("Couldn't check member {} for guild {}", member.getIdLong(), guild.getName(), exception);
            return false;
        }
    }

    /**
     * A list of all channel IDs for a given guild which have been blocklisted from the level system.
     * <p>
     * It is not guaranteed that the channel still exists in the Discord guild.
     *
     * @param guild the guild
     * @return a list of all channel ids. May be empty or immutable
     */
    public List<XPBlockData> getXPBlocks(Guild guild, int page) {
        List<XPBlockData> xpBlockedUsers = new ArrayList<>();

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM BlockedUsers WHERE guild_id = ? ORDER BY time DESC LIMIT 5 OFFSET least((? - 1) * 5, ceil((SELECT count(*) FROM BlockedUsers WHERE guild_id = ?) / 5) * 5)")) {
                statement.setLong(1, guild.getIdLong());
                statement.setInt(2, page);
                statement.setLong(3, guild.getIdLong());

                var resultSet = statement.executeQuery();
                while (resultSet.next() && xpBlockedUsers.size() < 5) {
                    XPBlockData xpBlockData = new XPBlockData(resultSet.getLong("user_id"), resultSet.getTimestamp("time").toLocalDateTime());
                    xpBlockedUsers.add(xpBlockData);
                }
            }
        } catch (SQLException exception) {
            logger.error("Couldn't check xp blocked users for guild {}", guild.getName(), exception);
            return List.of();
        }

        return xpBlockedUsers;
    }

    /**
     * Updates the block status of the provided channel in the guild.
     *
     * @param guild the guild
     * @param xpBlockData the channel's ID (the channel may no longer exist in the Discord guild)
     * @param blocked true if XP gain should be disabled in the provided channel, otherwise false
     * @return false if the update was unsuccessful or on database failure, otherwise true
     */
    public boolean setBlocked(Guild guild, XPBlockData xpBlockData, boolean blocked) {
        try (var connection = databaseManager.getConnection()) {
            if (blocked) {
                return block(guild, xpBlockData, connection);
            } else {
                return unblock(guild, xpBlockData.userID(), connection);
            }
        } catch (SQLException exception) {
            logger.info("Couldn't change xp block status of user {} for guild {}", xpBlockData.userID(), guild.getName());
            return false;
        }
    }

    private boolean block(Guild guild, XPBlockData xpBlockData, Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO BlockedUsers (guild_id, user_id, time) VALUES (?, ?, ?) ON CONFLICT (guild_id, user_id) DO NOTHING")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, xpBlockData.userID());
            statement.setTimestamp(3, new Timestamp(xpBlockData.timeInMillis()));
            return statement.executeUpdate() == 1;
        }
    }

    private boolean unblock(Guild guild, long userID, Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM BlockedUsers WHERE guild_id = ? AND user_id = ?")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, userID);
            return statement.executeUpdate() == 1;
        }
    }
}
