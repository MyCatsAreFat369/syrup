package org.maplestar.syrup.data.settings;

import net.dv8tion.jda.api.entities.Guild;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Provides access to the settings for each individual Discord guild.
 */
public class GuildSettingsManager {
    private final Logger logger = LoggerFactory.getLogger(GuildSettingsManager.class);
    private final DatabaseManager databaseManager;

    /**
     * Initializes the class.
     *
     * @param databaseManager the database manager for database access
     */
    public GuildSettingsManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Returns the guild settings associated with the provided guild.
     *
     * @param guild the guild
     * @return the settings for the guild or the default settings on database failure
     */
    public GuildSettings getSettings(Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT remove_old_roles, add_on_join FROM GuildSettings WHERE guild_id = ?")) {
                statement.setLong(1, guild.getIdLong());

                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new GuildSettings(resultSet.getBoolean("remove_old_roles"), resultSet.getBoolean("add_on_join"));
                } else {
                    return GuildSettings.DEFAULT;
                }
            }
        } catch (SQLException exception) {
            logger.error("Failed to access guild settings for guild {}", guild.getName(), exception);
            return GuildSettings.DEFAULT;
        }
    }

    /**
     * Updates the settings associated with the provided guild or inserts them into the database if necessary (upsert).
     *
     * @param guild the guild
     * @param guildSettings the new guild settings
     * @return false if the update was unsuccessful or on database failure, otherwise true
     */
    public boolean setSettings(Guild guild, GuildSettings guildSettings) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO GuildSettings (guild_id, remove_old_roles, add_on_join) VALUES (?, ?, ?) ON CONFLICT (guild_id) DO UPDATE SET remove_old_roles = EXCLUDED.remove_old_roles, add_on_join = EXCLUDED.add_on_join")) {
                statement.setLong(1, guild.getIdLong());
                statement.setBoolean(2, guildSettings.removeOldRoles());
                statement.setBoolean(3, guildSettings.addOnRejoin());
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Failed to set guild settings for guild {}", guild.getName(), exception);
            return false;
        }
    }
}
