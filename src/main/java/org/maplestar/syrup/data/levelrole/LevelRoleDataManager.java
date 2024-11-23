package org.maplestar.syrup.data.levelrole;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides access to the level roles for each individual Discord guild, which can be obtained by increasing your level.
 */
public class LevelRoleDataManager {
    private final Logger logger = LoggerFactory.getLogger(LevelRoleDataManager.class);
    private final DatabaseManager databaseManager;

    /**
     * Initializes the class
     *
     * @param databaseManager the database manager for database access
     */
    public LevelRoleDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * A list of all level roles for the provided guild with their Discord role IDs and associated levels.
     * <p>
     * It is not guaranteed that the Discord roles with the IDs still exist.
     *
     * @param guild the guild
     * @return a list of all level roles. May be empty or immutable
     */
    public List<LevelRoleData> getLevelRoles(Guild guild) {
        List<LevelRoleData> levelRoles = new ArrayList<>();

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT role_id, level FROM LevelRoles WHERE guild_id = ?")) {
                statement.setLong(1, guild.getIdLong());

                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    levelRoles.add(new LevelRoleData(resultSet.getLong("role_id"), resultSet.getInt("level")));
                }
            }
        } catch (SQLException exception) {
            logger.error("Failed to access level roles for guild {}", guild.getName(), exception);
            return List.of();
        }

        return levelRoles;
    }

    /**
     * Checks whether the provided role on the guild is a registered level role.
     *
     * @param role the role representing the level role
     * @param guild the guild
     * @return false if the role is not a level role or on database failure, otherwise true
     */
    public boolean isLevelRole(Role role, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM LevelRoles WHERE guild_id = ? AND role_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, role.getIdLong());
                return statement.executeQuery().next();
            }
        } catch (SQLException exception) {
            logger.error("Failed to check level role {} for guild {}", role.getId(), guild.getName(), exception);
            return false;
        }
    }

    /**
     * Retrieves the level at which the provided level role on the guild should be obtained.
     *
     * @param role the Discord role representing the level role
     * @param guild the guild
     * @return an empty {@link Optional} if the role is not a level role or on database failure, otherwise containing the level
     */
    public Optional<Integer> getLevel(Role role, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT level FROM LevelRoles WHERE guild_id = ? AND role_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, role.getIdLong());

                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(resultSet.getInt("level"));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException exception) {
            logger.error("Failed to check level for role {} in guild {}", role.getId(), guild.getName(), exception);
            return Optional.empty();
        }
    }

    /**
     * Registers a level role to be obtained at the specified level on the guild.
     *
     * @param guild the guild
     * @param levelRole the Discord role representing the level role
     * @param level the level
     * @return false if the update was unsuccessful or on database failure, otherwise true
     */
    public boolean addLevelRole(Guild guild, Role levelRole, int level) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO LevelRoles (guild_id, role_id, level) VALUES (?, ?, ?) ON CONFLICT DO NOTHING")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, levelRole.getIdLong());
                statement.setInt(3, level);
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Failed to add level role for guild {}", guild.getName(), exception);
            return false;
        }
    }

    /**
     * Removes a level role from the database so it can no longer be obtained.
     *
     * @param guild the guild
     * @param levelRoleID the ID of the Discord role representing this level role
     * @return false if the update was unsuccessful or on database failure, otherwise true
     */
    public boolean removeLevelRole(Guild guild, long levelRoleID) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("DELETE FROM LevelRoles WHERE guild_id = ? AND role_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, levelRoleID);
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Failed to remove level role for guild {}", guild.getName(), exception);
            return false;
        }
    }
}
