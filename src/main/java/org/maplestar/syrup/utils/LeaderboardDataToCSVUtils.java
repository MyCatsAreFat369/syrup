package org.maplestar.syrup.utils;

import org.maplestar.syrup.data.rank.RankingData;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for converting a list of {@link RankingData} to a CSV file.
 * Doesn't take the order of the ranks into account.
 */
public class LeaderboardDataToCSVUtils {
    /**
     * Converts the provided list of {@link RankingData} into a CSV byte array.
     *
     * @param data the ranking data
     * @return bytes representing the contents of the CSV
     */
    public static byte[] createCSVFileFromData(List<RankingData> data) {
        var contents = data.stream()
                .map(rankingData -> String.format("%s;%s;%s", rankingData.userID(), rankingData.levelData().level(), rankingData.levelData().xp()))
                .collect(Collectors.joining("\n"));

        return contents.getBytes();
    }
}
