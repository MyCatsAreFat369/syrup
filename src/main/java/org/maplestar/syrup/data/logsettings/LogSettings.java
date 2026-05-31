package org.maplestar.syrup.data.logsettings;

public record LogSettings(long channel_id, boolean logLeaderboardBackups, boolean logLevelroleAdded,
                          boolean logLevelroleRemoved)
{
    public static LogSettings DEFAULT = new LogSettings(-1, true, true, true);

    public LogSettings setChannelID(long value)
    {
        return new LogSettings(value, this.logLeaderboardBackups, this.logLevelroleAdded, this.logLevelroleRemoved);
    }

    public LogSettings setLeaderboardBackups(boolean value)
    {
        return new LogSettings(this.channel_id, value, this.logLevelroleAdded, this.logLevelroleRemoved);
    }

    public LogSettings setLevelroleAdded(boolean value)
    {
        return new LogSettings(this.channel_id, this.logLeaderboardBackups, value, this.logLevelroleRemoved);
    }

    public LogSettings setLevelroleRemoved(boolean value)
    {
        return new LogSettings(this.channel_id, this.logLeaderboardBackups, this.logLevelroleAdded, value);
    }
}
