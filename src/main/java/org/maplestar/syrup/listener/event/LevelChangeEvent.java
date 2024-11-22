package org.maplestar.syrup.listener.event;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.maplestar.syrup.data.rank.LevelData;

/**
 * Represents a level change of a user.
 * <p>
 * Note: Although the level will typically increase, it is possible for it to decrease.
 *
 * @param guild the guild the user leveled up in
 * @param user the user
 * @param newLevelData the user's current {@link LevelData}
 * @param oldLevelData the user's previous {@link LevelData}
 */
public record LevelChangeEvent(Guild guild, User user, LevelData newLevelData, LevelData oldLevelData) {
}
