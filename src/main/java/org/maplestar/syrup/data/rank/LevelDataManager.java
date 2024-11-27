package org.maplestar.syrup.data.rank;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides access to the level and XP of users for each guild.
 */
public class LevelDataManager {
    private final Logger logger = LoggerFactory.getLogger(LevelDataManager.class);
    private final DatabaseManager databaseManager;

    /**
     * Initializes the class
     *
     * @param databaseManager the database manager for database access
     */
    public LevelDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Returns the {@link LevelData} for the user on the provided guild.
     *
     * @param user the user
     * @param guild the guild
     * @return the {@link LevelData} for the user. May be the default for new users or on database failure
     */
    public LevelData getLevelData(User user, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT level, xp FROM Ranks WHERE user_id = ? AND guild_id = ?")) {
                statement.setLong(1, user.getIdLong());
                statement.setLong(2, guild.getIdLong());

                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new LevelData(resultSet.getInt("level"), resultSet.getLong("xp"));
                } else {
                    return LevelData.ZERO;
                }
            }
        } catch (SQLException exception) {
            logger.error("Couldn't access level data for user {} on guild {}", user.getName(), guild.getId(), exception);
            return LevelData.ZERO;
        }
    }

    /**
     * Returns the {@link RankingData} for the user on the provided guild, which includes a rank relative to other users.
     *
     * @param user the user
     * @param guild the guild
     * @return the {@link RankingData} for the user. May be the default for new users or on database failure
     */
    public RankingData getRankingData(User user, Guild guild) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT level, xp, rank FROM (SELECT *, rank() OVER (ORDER BY xp DESC) AS rank FROM Ranks WHERE guild_id = ?) WHERE user_id = ?")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, user.getIdLong());

                var resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    var levelData = new LevelData(resultSet.getInt("level"), resultSet.getLong("xp"));
                    return new RankingData(user.getIdLong(), resultSet.getInt("rank"), levelData);
                } else {
                    return RankingData.zero(user);
                }
            }
        } catch (SQLException exception) {
            logger.error("Couldn't access level data for user {} on guild {}", user.getName(), guild.getId(), exception);
            return RankingData.zero(user);
        }
    }

    /**
     * Returns a list of the top users in the specified guild, limited to 10 entries per page and offset by (page * 10) - 10.
     * <p>
     * Guaranteed to only be empty if there is no data for the provided guild.
     * If the page exceeds the bounds of the data, it is adjusted automatically
     *
     * @param guild the guild
     * @param page the page, adjusted automatically to fit the bounds
     * @return the list of users with their ranks. May be empty or contain less than 10 entries
     */
    public List<RankingData> getTopUsers(Guild guild, int page) {
        if (page < 1) page = 1;

        List<RankingData> result = new ArrayList<>();
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("SELECT user_id, level, xp, rank FROM (SELECT *, rank() OVER (ORDER BY xp DESC) AS rank FROM Ranks WHERE guild_id = ?) LIMIT 10 OFFSET least((? - 1) * 10, greatest(0, (SELECT count(*) FROM Ranks WHERE guild_id = ?) - 10))")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, page);
                statement.setLong(3, guild.getIdLong());

                var resultSet = statement.executeQuery();
                while (resultSet.next() && result.size() < 10) {
                    var levelData = new LevelData(resultSet.getInt("level"), resultSet.getLong("xp"));
                    result.add(new RankingData(resultSet.getLong("user_id"), resultSet.getInt("rank"), levelData));
                }

                return result;
            }
        } catch (SQLException exception) {
            logger.error("Couldn't access top users for guild {}", guild.getId(), exception);
            return List.of();
        }
    }

    /**
     * Updates the level and XP for the user on the specified guild or inserts them into the database if necessary (upsert).
     *
     * @param user the user
     * @param guild the guild
     * @param levelData the new {@link LevelData}
     */
    public boolean setLevelData(User user, Guild guild, LevelData levelData) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO Ranks (guild_id, user_id, level, xp) VALUES (?, ?, ?, ?) ON CONFLICT (guild_id, user_id) DO UPDATE SET level = EXCLUDED.level, xp = EXCLUDED.xp")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, user.getIdLong());
                statement.setInt(3, levelData.level());
                statement.setLong(4, levelData.xp());
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException exception) {
            logger.error("Couldn't update level data for user {} on guild {}", user.getName(), guild.getId(), exception);
            return false;
        }
    }
}
