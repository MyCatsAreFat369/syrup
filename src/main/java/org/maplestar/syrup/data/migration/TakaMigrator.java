package org.maplestar.syrup.data.migration;

import org.maplestar.syrup.data.DatabaseManager;
import org.maplestar.syrup.data.rank.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Migrates Taka's .csv level export files in the "migration" folder to syrup's database.
 */
public class TakaMigrator {
    private final static Logger logger = LoggerFactory.getLogger(TakaMigrator.class);

    /**
     * Scans the "migration" folder for .csv files and attempts to import them.
     *
     * @param databaseManager the database manager
     */
    public static void migrateTakaFiles(DatabaseManager databaseManager) {
        logger.info("Starting Taka migration...");

        try (var files = Files.walk(Path.of("migration"))) {
            files.filter(path -> path.toFile().getName().endsWith(".csv"))
                    .forEach(path -> migrateFile(path, databaseManager));
        } catch (IOException exception) {
            logger.error("Failed to migrate Taka files", exception);
        }

        logger.info("Finished Taka migration!!");
    }

    /**
     * Imports the provided .csv file into the database, discarding but warning about all invalid or duplicate data.
     * Afterward, the file is deleted automatically so it's only migrated once.
     * <p>
     * This method may take a while to run on larger files and will therefore delay the bot's startup.
     * However, it is run synchronously before any connection to Discord servers to ensure changes from Discord won't prevent the import.
     *
     * @param path the path of the .csv file
     * @param databaseManager the database manager
     */
    private static void migrateFile(Path path, DatabaseManager databaseManager) {
        logger.info("Importing Taka file {}", path);

        try {
            List<MigrationData> importData = Files.readAllLines(path).stream()
                    .filter(line -> line.matches("\\d+,\\d+,\\d+"))
                    .map(MigrationData::ofTaka)
                    .toList();

            logger.info("Finished parsing data, starting validation...");

            importData.stream()
                    .filter(migrationData -> migrationData.level() != LevelData.ZERO.setXP(migrationData.xp()).level())
                    .forEach(migrationData -> logger.warn("Found inconsistent data: {}. Expected level {}", migrationData, LevelData.ZERO.setXP(migrationData.xp()).level()));

            logger.info("Validated data, starting import into database...");

            int totalRows = 0;
            try (var connection = databaseManager.getConnection()) {
                for (var userData : importData) {
                    try (var statement = connection.prepareStatement("INSERT INTO Ranks (guild_id, user_id, level, xp) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")) {
                        statement.setLong(1, Long.parseLong(path.toFile().getName().replace(".csv", "")));
                        statement.setLong(2, userData.userID());
                        statement.setInt(3, userData.level());
                        statement.setLong(4, userData.xp());

                        var updatedRows = statement.executeUpdate();
                        if (updatedRows == 1) {
                            totalRows++;
                        } else {
                            logger.error("For user data {}, {} rows (expected: 1) were updated", userData, updatedRows);
                        }
                    }
                }
            }

            logger.info("Migration finished, imported {} of {} user ranks", totalRows, importData.size());

            var fileDeletionSuccess = path.toFile().delete();
            if (!fileDeletionSuccess) {
                logger.warn("Failed to delete file {}, please remove it manually", path);
            }
        } catch (Exception exception) {
            logger.error("Failed to migrate Taka file {}", path, exception);
        }
    }
}
