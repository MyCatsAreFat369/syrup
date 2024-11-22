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

public class LevelRoleDataManager {
    private final Logger logger = LoggerFactory.getLogger(LevelRoleDataManager.class);
    private final DatabaseManager databaseManager;

    public LevelRoleDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

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
