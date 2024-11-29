package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.listener.event.LevelChangeEvent;

public class GuildMemberJoinListener extends ListenerAdapter {
    private final GuildSettingsManager guildSettingsManager;
    private final LevelDataManager levelDataManager;
    private final LevelChangeListener levelChangeListener;

    public GuildMemberJoinListener(GuildSettingsManager guildSettingsManager, LevelDataManager levelDataManager, LevelChangeListener levelChangeListener) {
        this.guildSettingsManager = guildSettingsManager;
        this.levelDataManager = levelDataManager;
        this.levelChangeListener = levelChangeListener;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        var user = event.getUser();
        var guild = event.getGuild();

        var guildSettings = guildSettingsManager.getSettings(guild);
        if (!guildSettings.addOnRejoin()) return;

        var levelData = levelDataManager.getLevelData(user, guild);
        if (levelData.level() != 0) {
            levelChangeListener.onLevelChange(new LevelChangeEvent(guild, user, LevelData.ZERO, levelData));
        }
    }
}
