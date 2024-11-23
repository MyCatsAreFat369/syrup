package org.maplestar.syrup.data.rank;

/**
 * Represents a user's level and XP in a guild.
 *
 * @param level the level
 * @param xp the XP
 */
public record LevelData(int level, long xp) {
    public static LevelData ZERO = new LevelData(0, 0);

    /**
     * Adds the specified amount of XP and recalculates the level if necessary.
     *
     * @param xp the XP amount
     * @return a new {@link LevelData} representing the new XP amount
     */
    public LevelData addXP(int xp) {
        var newXp = this.xp + xp;
        var requiredXP = requiredForLevelup(this.level);
        var newLevel = newXp >= requiredXP ? this.level + 1 : this.level;
        return new LevelData(newLevel, newXp);
    }

    /**
     * Sets the specified XP amount and calculates the corresponding level.
     *
     * @param xp the new XP amount
     * @return a new {@link LevelData} representing the new XP amount
     */
    public LevelData setXP(int xp) {
        if(xp < 100) return new LevelData(0, xp);

        int newLevel = xpToLevel();
        return new LevelData(newLevel, xp);
    }

    /**
     * Sets the specified level and calculates the corresponding XP amount.
     *
     * @param level the new level
     * @return a new {@link LevelData} representing the new level
     */
    public LevelData setLevel(int level) {
        if(level < 0) return this;
        return new LevelData(level, requiredForLevelup(level - 1));
    }

    /**
     * Calculates how much XP is required for achieve the next level.
     * <p>
     * Uses the formula (72 * level^2) + (50 * level) + 100
     *
     * @param level the current level
     * @return the amount of XP required
     */
    public long requiredForLevelup(int level) {
        if(level < 0) return 0;
        return (long) (72L * Math.pow(level, 2) + 50L * level + 100L);
    }

    /**
     * Calculates the level with the current XP amount.
     * <p>
     * Uses the formula floor(-50 + sqrt(50^2 - 4 * 72 * (100 - xp)) / (2 * 72)) + 1
     *
     * @return the level
     */
    public int xpToLevel() {
        return (int) Math.floor((-50 + Math.sqrt(2500 - 4 * 72 * (100 - xp))) / (2 * 72)) + 1;
    }
}
