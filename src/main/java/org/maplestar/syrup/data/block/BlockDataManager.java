package org.maplestar.syrup.data.block;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BlockDataManager {
    private final Logger logger = LoggerFactory.getLogger(BlockDataManager.class);
    private final DatabaseManager databaseManager;

    public BlockDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean isBlocked(Channel channel, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM BlockedChannels WHERE guild_id = ? AND channel_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, channel.getIdLong());
                return statement.executeQuery().next();
            }
        } catch (SQLException exception) {
            logger.error("Couldn't check channel {} for guild {}", channel.getId(), guild.getName(), exception);
            return false;
        }
    }

    public List<Long> getBlockedChannelIds(Guild guild) {
        List<Long> blockedChannels = new ArrayList<>();

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT channel_id FROM BlockedChannels WHERE guild_id = ?")) {
                statement.setLong(1, guild.getIdLong());

                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    blockedChannels.add(resultSet.getLong("channel_id"));
                }
            }
        } catch (SQLException exception) {
            logger.error("Couldn't check blocked channels for guild {}", guild.getName(), exception);
            return List.of();
        }

        return blockedChannels;
    }

    public boolean setBlocked(long channelID, Guild guild, boolean blocked) {
        try (var connection = databaseManager.getConnection()) {
            if (blocked) {
                return block(channelID, guild, connection);
            } else {
                return unblock(channelID, guild, connection);
            }
        } catch (SQLException exception) {
            logger.info("Couldn't change block status of channel {} for guild {}", channelID, guild.getName());
            return false;
        }
    }

    private boolean block(long channelID, Guild guild, Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO BlockedChannels (guild_id, channel_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, channelID);
            return statement.executeUpdate() == 1;
        }
    }

    private boolean unblock(long channelID, Guild guild, Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM BlockedChannels WHERE guild_id = ? AND channel_id = ?")) {
            statement.setLong(1, guild.getIdLong());
            statement.setLong(2, channelID);
            return statement.executeUpdate() == 1;
        }
    }
}
