package org.maplestar.syrup.data.rank;

public record LevelData(int level, long xp) {
    public static LevelData ZERO = new LevelData(0, 0);

    public LevelData addXP(int xp) {
        var newXp = this.xp + xp;
        var requiredXP = requiredForLevelup(this.level);
        var newLevel = newXp >= requiredXP ? this.level + 1 : this.level;
        return new LevelData(newLevel, newXp);
    }
    
    public LevelData setXP(int xp) {
        if(xp < 100) return new LevelData(0, xp);

        int newLevel = xpToLevel();
        return new LevelData(newLevel, xp);
    }

    public LevelData setLevel(int level) {
        if(level < 0) return this;
        return new LevelData(level, requiredForLevelup(level - 1));
    }
    
    public long requiredForLevelup(int level) {
        if(level < 0) return 0;
        return (long) (72L * Math.pow(level, 2) + 50L * level + 100L);
    }

    public int xpToLevel() {
        return (int) Math.floor((-50 + Math.sqrt(2500 - 4 * 72 * (100 - xp))) / (2 * 72)) + 1;
    }
}
