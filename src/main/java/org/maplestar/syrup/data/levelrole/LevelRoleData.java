package org.maplestar.syrup.data.levelrole;

/**
 * Represents a level role that can be obtained as a user gains levels in the rank system.
 * <p>
 * The role may no longer exist on the Discord guild.
 *
 * @param roleID the Discord role's ID
 * @param level the level associated with the role
 */
public record LevelRoleData(long roleID, int level) {
}
