package org.maplestar.syrup.data.requestdata;

import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.xpblock.XPBlockData;

public record RequestGuildPack(LevelData levelData, XPBlockData xpBlockData)
{
    public RequestGuildPack setLevelData(LevelData levelData)
    {
        return new RequestGuildPack(levelData, this.xpBlockData);
    }

    public RequestGuildPack setXPBlockData(XPBlockData xpBlockData)
    {
        return new RequestGuildPack(this.levelData, xpBlockData);
    }
}
