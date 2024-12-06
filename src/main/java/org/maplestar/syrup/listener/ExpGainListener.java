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

/**
 * Event listener that's called when a message is sent and handles XP updates.
 */
public class ExpGainListener extends ListenerAdapter {
    private final LevelDataManager levelDataManager;
    private final BlockDataManager blockDataManager;
    private final LevelChangeListener levelChangeListener;
    private final CooldownProvider<User> cooldownProvider = CooldownProvider.withDuration(Duration.ofSeconds(10));

    /**
     * Initializes the class.
     *
     * @param levelDataManager the level data manager
     * @param blockDataManager the block data manager
     * @param levelChangeListener the level change listener, to notify when a user levels up
     */
    public ExpGainListener(LevelDataManager levelDataManager, BlockDataManager blockDataManager, LevelChangeListener levelChangeListener) {
        this.levelDataManager = levelDataManager;
        this.blockDataManager = blockDataManager;
        this.levelChangeListener = levelChangeListener;
    }

    /**
     * Called when the bot receives a message either in a guild or private message.
     * <p>
     * Used to add XP to the user in case they're writing in a non-blocked guild channel and aren't on cooldown.
     *
     * @param event the event that has been fired
     * @see BlockDataManager
     */
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

        levelDataManager.setLevelData(event.getAuthor(), event.getGuild(), newLevelData);

        if (newLevelData.level() != oldLevelData.level()) {
            levelChangeListener.onLevelChange(new LevelChangeEvent(guild, user, newLevelData, oldLevelData));
        }
    }
}
