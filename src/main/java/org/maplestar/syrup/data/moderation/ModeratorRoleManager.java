package org.maplestar.syrup.data.moderation;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModeratorRoleManager {
    private final Logger logger = LoggerFactory.getLogger(ModeratorRoleManager.class);
    private final DatabaseManager databaseManager;

    public ModeratorRoleManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<Long> getModeratorRoles(Guild guild) {
        List<Long> moderatorRoles = new ArrayList<>();

        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT role_id FROM ModeratorRoles WHERE guild_id = ?")) {
                statement.setLong(1, guild.getIdLong());

                var resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    moderatorRoles.add(resultSet.getLong("role_id"));
                }
            }
        } catch (SQLException exception) {
            logger.error("Failed to check moderator roles in guild {}", guild.getName(), exception);
            return List.of();
        }

        return moderatorRoles;
    }

    public boolean isModeratorRole(Role role, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT * FROM ModeratorRoles WHERE guild_id = ? AND role_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, role.getIdLong());
                return statement.executeQuery().next();
            }
        } catch (SQLException exception) {
            logger.error("Failed to check moderator role {} in guild {}", role.getId(), guild.getName(), exception);
            return false;
        }
    }

    public boolean addModeratorRole(Role role, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO ModeratorRoles (guild_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, role.getIdLong());
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Failed to add moderator role {} in guild {}", role.getId(), guild.getName(), exception);
            return false;
        }
    }

    public boolean removeModeratorRole(long roleID, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("DELETE FROM ModeratorRoles WHERE guild_id = ? AND role_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, roleID);
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Failed to remove moderator role {} in guild {}", roleID, guild.getName(), exception);
            return false;
        }
    }
}
