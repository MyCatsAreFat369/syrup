package org.maplestar.syrup.data.settings;

/**
 * Represents the settings of a single guild.
 *
 * @param removeOldRoles whether level roles should be removed when a user acquires a higher one
 * @param addOnRejoin whether level roles should be added back when a user re-joins the server
 */
public record GuildSettings(boolean removeOldRoles, boolean addOnRejoin) {
    /**
     * The default guild settings to be used for new guilds and in case of database failure.
     */
    public static final GuildSettings DEFAULT = new GuildSettings(true, true);

    /**
     * Updates whether level roles should be removed when a user acquires a higher one.
     *
     * @param value true if level roles should be removed
     * @return the updated guild settings
     */
    public GuildSettings setRemoveOldRoles(boolean value) {
        return new GuildSettings(value, this.addOnRejoin);
    }

    /**
     * Updates whether level roles should be added back when a user re-joins the server.
     *
     * @param value true if roles should be re-added
     * @return whether level roles should be added back when a user re-joins the server
     */
    public GuildSettings setAddOnRejoin(boolean value) {
        return new GuildSettings(this.removeOldRoles, value);
    }
}
