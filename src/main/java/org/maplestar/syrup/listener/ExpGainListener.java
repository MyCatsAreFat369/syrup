package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.maplestar.syrup.data.block.BlockDataManager;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.listener.event.LevelChangeEvent;
import org.maplestar.syrup.utils.CooldownProvider;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class ExpGainListener extends ListenerAdapter {
    private final LevelDataManager levelDataManager;
    private final BlockDataManager blockDataManager;
    private final LevelChangeListener levelChangeListener;
    private final CooldownProvider<User> cooldownProvider = CooldownProvider.withDuration(Duration.ofSeconds(10));

    public ExpGainListener(LevelDataManager levelDataManager, BlockDataManager blockDataManager, LevelChangeListener levelChangeListener) {
        this.levelDataManager = levelDataManager;
        this.blockDataManager = blockDataManager;
        this.levelChangeListener = levelChangeListener;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;
        if (blockDataManager.isBlocked(event.getChannel(), event.getGuild())) return;

        var guild = event.getGuild();
        var user = event.getAuthor();

        if (cooldownProvider.isOnCooldown(user)) return;
        cooldownProvider.applyCooldown(user);

        var rand = ThreadLocalRandom.current();
        int addXP = rand.nextInt(15, 31);
        var oldLevelData = levelDataManager.getLevelData(user, guild);
        var newLevelData = oldLevelData.addXP(addXP);

        if (newLevelData.level() != oldLevelData.level()) {
            levelChangeListener.onLevelChange(new LevelChangeEvent(guild, user, newLevelData, oldLevelData));
        }

        levelDataManager.setLevelData(event.getAuthor(), event.getGuild(), oldLevelData);
    }
}
