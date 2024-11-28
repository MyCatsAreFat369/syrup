package org.maplestar.syrup.data.migration;

import org.maplestar.syrup.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TakaMigrator {
    private final static Logger logger = LoggerFactory.getLogger(TakaMigrator.class);

    public static void migrateTakaFiles(DatabaseManager databaseManager) {
        logger.info("Starting Taka migration...");

        logger.info(String.valueOf(Path.of("Migration").toAbsolutePath()));
        try (var files = Files.walk(Path.of("Migration"))) {
            files.filter(path -> path.toFile().getName().endsWith(".csv"))
                    .forEach(path -> migrateFile(path, databaseManager));
        } catch (IOException exception) {
            logger.error("Failed to migrate Taka files", exception);
        }
    }

    private static void migrateFile(Path path, DatabaseManager databaseManager) {
        logger.info("Importing Taka file {}", path);

        try {
            List<MigrationData> importData = Files.readAllLines(path).stream()
                    .filter(line -> line.matches("\\d+,\\d+,\\d+"))
                    .map(MigrationData::of)
                    .toList();

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