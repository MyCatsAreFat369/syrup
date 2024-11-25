package org.maplestar.syrup.listener;

import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.listener.event.LevelChangeEvent;

import java.util.List;

// oh my god our own event???

/**
 * Listener that's called when the level of a user gets updated.
 */
public class LevelChangeListener {
    private final LevelRoleDataManager levelRoleDataManager;
    private final GuildSettingsManager guildSettingsManager;

    /**
     * Initializes the class.
     *
     * @param levelRoleDataManager the level role manager
     * @param guildSettingsManager the guild settings manager
     */
    public LevelChangeListener(LevelRoleDataManager levelRoleDataManager, GuildSettingsManager guildSettingsManager) {
        this.levelRoleDataManager = levelRoleDataManager;
        this.guildSettingsManager = guildSettingsManager;
    }

    /**
     * Called when the level of a user gets updated.
     *
     * @param event the event that has been fired, see {@link LevelChangeEvent}
     */
    public void onLevelChange(LevelChangeEvent event)
    {
        var guild = event.guild();
        var settings = guildSettingsManager.getSettings(guild);
        var roles = levelRoleDataManager.getLevelRoles(guild);

        var member = guild.retrieveMember(event.user()).submit().join();

        // if rank increases, consider role: oldLevel < role.level <= newLevel
        // if rank decreases, consider role: newLevel < role.level <= oldLevel

        int oldLevel = event.oldLevelData().level();
        int newLevel = event.newLevelData().level();

        int minLevel = Math.min(oldLevel, newLevel);
        int maxLevel = Math.max(oldLevel, newLevel);

        // gets all roles within the range of minLevel and maxLevel
        var affectedRoles = roles.stream()
                .filter(levelRole -> minLevel < levelRole.level() && levelRole.level() <= maxLevel)
                .toList();

        // if level change doesn't actually affect any roles, checks if user doesn't have any roles below minLevel
        // if removeOldRoles then only the largest such existing role will be added.
        if (affectedRoles.isEmpty())
        {
            var lowerRoles = roles.stream()
                    .filter(levelRole -> levelRole.level() <= minLevel)
                    .toList();

            // guess we didn't have any lower roles either, returns
            if(lowerRoles.isEmpty()) return;

            // add all lower roles because removeOldRoles is false
            if(!settings.removeOldRoles())
            {
                var lowerRolesList = lowerRoles.stream()
                        .map(levelRole -> guild.getRoleById(levelRole.roleID()))
                        .toList();
                guild.modifyMemberRoles(member, lowerRolesList, null).queue();
                return;
            }

            // get the max lower role level and add that role to member
            var maxLevelRole = lowerRoles.getFirst();
            for(var levelRole : lowerRoles)
            {
                if(levelRole.level() > maxLevelRole.level()) maxLevelRole = levelRole;
            }
            guild.modifyMemberRoles(member, List.of(guild.getRoleById(maxLevelRole.roleID())), null).queue();

            return;
        }

        // removes ALL old roles (if removeOldRoles)
        if (settings.removeOldRoles()) {
            for (var levelRole : roles) {
                var role = guild.getRoleById(levelRole.roleID());
                if (role == null) continue;
                guild.modifyMemberRoles(member, null, List.of(role)).queue();
            }
        }

        int maxLevelRole = 0;

        // finds the maximum role level amongst all the roles
        for (var levelRole : affectedRoles) {
            if (levelRole.level() > maxLevelRole) maxLevelRole = levelRole.level();
        }

        // either adds new role(s) or removes them depending on if level increased or decreased
        for (var levelRole : affectedRoles) {
            var role = guild.getRoleById(levelRole.roleID());
            if (role == null) continue;

            // adds roles
            if (oldLevel < newLevel) {
                // if removeOldRoles and not the max role level then don't add the roles
                if (settings.removeOldRoles() && levelRole.level() != maxLevelRole) continue;
                guild.modifyMemberRoles(member, List.of(role), null).queue();
            } else {
                // if removeOldRoles then roles were already removed earlier
                if (settings.removeOldRoles()) continue;
                guild.modifyMemberRoles(member, null, List.of(role)).queue();
            }
        }
    }
}
