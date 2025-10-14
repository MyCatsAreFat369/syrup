package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.listener.event.LevelChangeEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Event listener that's called when a new member joins the guild.
 * Used to automatically re-apply roles if the guild settings are configured that way.
 */
public class GuildMemberJoinListener extends ListenerAdapter {
    private final GuildSettingsManager guildSettingsManager;
    private final LevelDataManager levelDataManager;
    private final LevelChangeListener levelChangeListener;
    private final LevelRoleDataManager levelRoleDataManager;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Initializes the class.
     *
     * @param guildSettingsManager the guild settings manager
     * @param levelDataManager the level data manager
     * @param levelChangeListener the level change listener, to notify of the indirect level up
     * @param levelRoleDataManager the level role data manager
     */
    public GuildMemberJoinListener(GuildSettingsManager guildSettingsManager, LevelDataManager levelDataManager,
                                   LevelChangeListener levelChangeListener, LevelRoleDataManager levelRoleDataManager) {
        this.guildSettingsManager = guildSettingsManager;
        this.levelDataManager = levelDataManager;
        this.levelChangeListener = levelChangeListener;
        this.levelRoleDataManager = levelRoleDataManager;
    }

    /**
     * Called when a member joins a guild. Re-assigns level roles according to the guild settings.
     *
     * @param event the event that has been fired
     * @see org.maplestar.syrup.data.settings.GuildSettings
     */
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        executor.schedule(() -> {
            var user = event.getUser();
            var guild = event.getGuild();

            var guildSettings = guildSettingsManager.getSettings(guild);
            if (!guildSettings.addOnRejoin()) return;

            var levelData = levelDataManager.getLevelData(user, guild);
            if (levelData.level() == 0) {
                applyDefaultRole(guild, user);
            } else {
                levelChangeListener.onLevelChange(new LevelChangeEvent(guild, user, LevelData.ZERO, levelData));
            }
        }, 10, TimeUnit.SECONDS);
    }

    private void applyDefaultRole(Guild guild, User user) {
        var levelRoles = levelRoleDataManager.getLevelRoles(guild);
        for (var levelRole : levelRoles) {
            if (levelRole.level() == 0) {
                var role = guild.getRoleById(levelRole.roleID());
                if (role == null) return;

                guild.addRoleToMember(user, role).queue();
                return;
            }
        }
    }
}
