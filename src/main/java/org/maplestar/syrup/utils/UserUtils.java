package org.maplestar.syrup.utils;

import net.dv8tion.jda.api.entities.Guild;

/**
 * Utility class for JDA's {@link net.dv8tion.jda.api.entities.User}.
 */
public class UserUtils {
    /**
     * Attempts to fetch the username from the Discord API.
     * Since this might take a while, it's advised to call this outside the main thread.
     *
     * @param guild a guild this user is one
     * @param userID the user id
     * @return the name of the user or a mention as a fallback
     */
    public static String getNameByUserID(Guild guild, long userID) {
        return String.format("%.18s", guild.retrieveMemberById(userID).submit().join().getUser().getName());
    }
}
