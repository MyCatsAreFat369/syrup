package org.maplestar.syrup.data.rank;

import net.dv8tion.jda.api.entities.User;

public record RankingData(long userID, int rank, LevelData levelData) {
    public static RankingData zero(User user) {
        return new RankingData(user.getIdLong(), -1, LevelData.ZERO);
    }
}
