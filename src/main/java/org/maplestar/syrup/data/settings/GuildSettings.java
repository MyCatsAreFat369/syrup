package org.maplestar.syrup.data.settings;

public record GuildSettings(boolean removeOldRoles, boolean addOnRejoin) {
    public static final GuildSettings DEFAULT = new GuildSettings(true, true);

    public GuildSettings setRemoveOldRoles(boolean value) {
        return new GuildSettings(value, this.addOnRejoin);
    }

    public GuildSettings setAddOnRejoin(boolean value) {
        return new GuildSettings(this.removeOldRoles, value);
    }
}
