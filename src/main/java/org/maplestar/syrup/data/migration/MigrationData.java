package org.maplestar.syrup.data.migration;

public record MigrationData(long userID, long xp, int level) {
    public static MigrationData of(String line) throws IllegalArgumentException {
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
