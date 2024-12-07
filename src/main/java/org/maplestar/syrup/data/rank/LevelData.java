package org.maplestar.syrup.data.rank;

/**
 * Represents a user's level and XP in a guild.
 *
 * @param level the level
 * @param xp the XP
 */
public record LevelData(int level, long xp) {
    public static final LevelData ZERO = new LevelData(0, 0);
    public static final LevelData MAX = LevelData.ZERO.setLevel(420);

    /**
     * Adds the specified amount of XP and recalculates the level if necessary.
     *
     * @param xp the XP amount
     * @return a new {@link LevelData} representing the new XP amount
     */
    public LevelData addXP(long xp) {
        var newXp = this.xp + xp;
        var requiredXP = requiredTotalForLevelup(this.level);
        var newLevel = newXp >= requiredXP ? this.level + 1 : this.level;
        return new LevelData(newLevel, newXp);
    }

    /**
     * Sets the specified XP amount and calculates the corresponding level.
     *
     * @param xp the new XP amount
     * @return a new {@link LevelData} representing the new XP amount
     */
    public LevelData setXP(long xp) {
        if (xp < 100) return new LevelData(0, xp);

        int newLevel = xpToLevel(xp);
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
        return new LevelData(level, requiredTotalForLevelup(level - 1));
    }

    /**
     * Calculates how much total XP is required to reach the next level.
     * <p>
     * Uses the formula (72 * level^2) + (50 * level) + 100
     *
     * @param level the current level
     * @return the amount of XP required
     */
    public long requiredTotalForLevelup(int level) {
        if(level < 0) return 0;
        return (long) (72L * Math.pow(level, 2) + 50L * level + 100L);
    }

    /**
     * Calculates how much more XP is required to reach the next level from the current one given the current XP amount.
     * <p>
     * Uses the formula total_xp_required_for_levelup - current_xp
     *
     * @return the amount of XP required
     */
    public long remainingXPForLevelup() {
        return requiredTotalForLevelup(level) - xp;
    }

    /**
     * Calculates the level with the current XP amount.
     * <p>
     * Uses the formula floor(-50 + sqrt(50^2 - 4 * 72 * (100 - xp)) / (2 * 72)) + 1
     *
     * @return the level
     */
    public int xpToLevel(long xp) {
        return (int) Math.floor((-50 + Math.sqrt(2500 - 4 * 72 * (100 - xp))) / (2 * 72)) + 1;
    }
}
