package org.maplestar.syrup.utils;

import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.RankingData;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

    public static List<RankingData> createDataFromCSVFile(File file)
    {
        List<RankingData> result = new ArrayList<>();
        try(Scanner myReader = new Scanner(file))
        {
            while(myReader.hasNextLine())
            {
                String data = myReader.nextLine();
                String[] dataSplit = data.split(";");
                RankingData rankingData = new RankingData(Long.parseLong(dataSplit[0]), 0,
                        new LevelData(Integer.parseInt(dataSplit[1]), Integer.parseInt(dataSplit[2])));
                result.add(rankingData);
            }
            return result;
        } catch(FileNotFoundException exception)
        {
            System.out.println("Couldn't read from file " + file.getAbsolutePath());
            exception.printStackTrace();
            return result;
        }
    }
}
