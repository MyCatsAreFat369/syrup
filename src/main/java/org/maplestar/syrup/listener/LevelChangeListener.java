package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.maplestar.syrup.data.levelrole.LevelRoleData;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.settings.GuildSettings;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.listener.event.LevelChangeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// oh my god our own event???

/**
 * Event listener that's called when the level of a user gets updated.
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
    public void onLevelChange(LevelChangeEvent event) {
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

        Set<Role> newRoles = new HashSet<>();
        Set<Role> removalRoles = new HashSet<>();

        // removes ALL old roles (if removeOldRoles)
        if (settings.removeOldRoles()) {
            roles.stream()
                    .map(levelRole -> guild.getRoleById(levelRole.roleID()))
                    .filter(Objects::nonNull)
                    .filter(member.getRoles()::contains)
                    .forEach(removalRoles::add);
        }

        // if level change doesn't affect any roles run this
        // if level decreases, also run this
        // if the former, this will simply add all of the lower roles or just the max, depending on removeOldRoles
        // if the latter, this will also remove all affected roles that shouldn't be on the user anymore because the level decreased
        // if both... no affected roles are actually affected and lower roles are added.
        if (affectedRoles.isEmpty() || newLevel < oldLevel) {
            // either affectedRoles is empty and this does nothing or newLevel is less than oldLevel and
            // we should indeed be removing affectedRoles
            affectedRoles.stream()
                    .map(levelRole -> guild.getRoleById(levelRole.roleID()))
                    .filter(Objects::nonNull)
                    .forEach(removalRoles::add);

            // Processing lower roles
            var lowerRoles = roles.stream()
                    .filter(levelRole -> levelRole.level() <= minLevel)
                    .toList();

            // add all lower roles because removeOldRoles is false
            if (!lowerRoles.isEmpty()) {
                newRoles.addAll(getRolesToAdd(guild, settings, lowerRoles));
            }

            // newRoles overrides removalRoles in case of conflicts
            removalRoles.removeIf(role -> newRoles.contains(role) || !member.getRoles().contains(role));
            newRoles.removeIf(member.getRoles()::contains);
            guild.modifyMemberRoles(member, newRoles, removalRoles).queue();
            return;
        }

        // add all affected roles because removeOldRoles is false
        newRoles.addAll(getRolesToAdd(guild, settings, affectedRoles));

        removalRoles.removeIf(role -> newRoles.contains(role) || !member.getRoles().contains(role));
        newRoles.removeIf(member.getRoles()::contains);
        guild.modifyMemberRoles(member, newRoles, removalRoles).queue();
    }

    /**
     * Gets max role if removeOldRoles, else gets all roles, of the rolesToProcess argument.
     * The list should then be added to newRoles, in order to give the necessary roles to the user.
     * <p>
     * I'm on television!! -maple
     *
     * @param guild the guild
     * @param settings the guild's settings
     * @param rolesToProcess quite literally, the roles to process
     * @return a list of roles given the rules explained above. This list should be added to newRoles
     */
    private List<Role> getRolesToAdd(Guild guild, GuildSettings settings, List<LevelRoleData> rolesToProcess) {
        if (settings.removeOldRoles()) {
            // get the max lower role level and add that role to member
            var maxLevelRole = rolesToProcess.getFirst();
            for(var levelRole : rolesToProcess) {
                if(levelRole.level() > maxLevelRole.level()) maxLevelRole = levelRole;
            }

            var theRole = guild.getRoleById(maxLevelRole.roleID());
            if (theRole == null) return List.of();
            return List.of(theRole);
        } else {
            return rolesToProcess.stream()
                    .map(levelRole -> guild.getRoleById(levelRole.roleID()))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
