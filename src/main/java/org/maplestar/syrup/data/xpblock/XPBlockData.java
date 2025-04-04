package org.maplestar.syrup.data.xpblock;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record XPBlockData(long userID, LocalDateTime time) {
    public long timeInMillis() {
        return time.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli();
    }
}
