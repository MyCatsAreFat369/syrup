package org.maplestar.syrup.data.xpblock;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record XPBlockData(long guildID, long userID, LocalDateTime time)
{
    public static final XPBlockData DEFAULT = new XPBlockData(-1, -1, LocalDateTime.MIN);
    public boolean isDefault()
    {
        return this.equals(DEFAULT);
    }
    public long timeInMillis()
    {
        return time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli();
    }
}
