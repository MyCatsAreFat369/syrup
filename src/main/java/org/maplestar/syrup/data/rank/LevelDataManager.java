package org.maplestar.syrup.data.rank;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LevelDataManager {
    private final Logger logger = LoggerFactory.getLogger(LevelDataManager.class);
    private final DatabaseManager databaseManager;

    public LevelDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

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

    public void setLevelData(User user, Guild guild, LevelData levelData) {
        try (var connection = databaseManager.getConnection()) {
            try (var statement = connection.prepareStatement("INSERT INTO Ranks (guild_id, user_id, level, xp) VALUES (?, ?, ?, ?) ON CONFLICT (guild_id, user_id) DO UPDATE SET level = EXCLUDED.level, xp = EXCLUDED.xp")) {
                statement.setLong(1, guild.getIdLong());
                statement.setLong(2, user.getIdLong());
                statement.setInt(3, levelData.level());
                statement.setLong(4, levelData.xp());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            logger.error("Couldn't update level data for user {} on guild {}", user.getName(), guild.getId(), exception);
        }
    }
}
