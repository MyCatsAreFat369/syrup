package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.maplestar.syrup.data.block.BlockDataManager;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildLeaveListener extends ListenerAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(GuildLeaveListener.class);
    private final BlockDataManager blockDataManager;
    private final LevelRoleDataManager levelRoleDataManager;
    private final GuildSettingsManager guildSettingsManager;
    private final XPBlockDataManager xpBlockDataManager;
    public GuildLeaveListener(BlockDataManager blockDataManager, LevelRoleDataManager levelRoleDataManager,
                              GuildSettingsManager guildSettingsManager, XPBlockDataManager xpBlockDataManager)
    {
        this.blockDataManager = blockDataManager;
        this.levelRoleDataManager = levelRoleDataManager;
        this.guildSettingsManager = guildSettingsManager;
        this.xpBlockDataManager = xpBlockDataManager;
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event)
    {
        var guild = event.getGuild();

        logger.info("Guild {} kicked me out!", guild.getIdLong());

        boolean success1 = blockDataManager.clearBlocks(guild);
        boolean success2 = levelRoleDataManager.clearLevelRoles(guild);
        boolean success3 = guildSettingsManager.clearGuildSettings(guild);
        boolean success4 = xpBlockDataManager.clearBlocks(guild);

        logger.info("Channel XP Block clear {}", success1 ? "successful" : "failed");
        logger.info("Level Role clear {}", success2 ? "successful" : "failed");
        logger.info("Guild Settings clear {}", success3 ? "successful" : "failed");
        logger.info("User XP Block clear {}", success4 ? "successful" : "failed");
    }
}
