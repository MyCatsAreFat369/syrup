package org.maplestar.syrup.data.migration;

/**
 * Level data for each user to be migrated to the bot's database.
 *
 * @param userID the ID of the user
 * @param xp the xp of the user
 * @param level the level of the user
 * @see org.maplestar.syrup.data.rank.LevelData
 */
public record MigrationData(long userID, long xp, int level) {
    /**
     * Attempts to convert the provided level data from Taka's CSV to a {@link MigrationData}.
     *
     * @param line the line to migrate
     * @return a new {@link MigrationData} instance representing the level data
     * @throws IllegalArgumentException if the line had an invalid format
     */
    public static MigrationData ofTaka(String line) throws IllegalArgumentException {
        try {
            String[] entriesStr = line.split(",");
            long userID = Long.parseLong(entriesStr[0]);
            long xp = Long.parseLong(entriesStr[1]);
            int level = Integer.parseInt(entriesStr[2]);
            return new MigrationData(userID, xp, level);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse migration data \"" + line + "\"", exception);
        }
    }
}
