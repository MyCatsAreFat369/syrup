package org.maplestar.syrup.data.rank;

import net.dv8tion.jda.api.entities.User;

/**
 * Represents a user's rank in a guild relative to other users.
 *
 * @param userID the user's ID
 * @param rank the user's rank
 * @param levelData the {@link LevelData} with information about the user's level and xp
 */
public record RankingData(long userID, int rank, LevelData levelData) {
    /**
     * An empty data record to be used in case of database failure or if a user hasn't sent any messages.
     *
     * @param user the user
     * @return a new instance representing invalid data (rank -1 and no {@link LevelData})
     */
    public static RankingData zero(User user) {
        return new RankingData(user.getIdLong(), -1, LevelData.ZERO);
    }

    /**
     * Indicates whether this object describes a valid rank.
     *
     * @return true if the data is valid, false if the user hasn't sent a message or on database connection failure
     */
    public boolean isInvalid() {
        return rank == -1;
    }
}
