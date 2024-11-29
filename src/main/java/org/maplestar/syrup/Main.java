package org.maplestar.syrup;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.maplestar.syrup.commands.LevelRoleCommand;
import org.maplestar.syrup.commands.RankCommand;
import org.maplestar.syrup.commands.XPBlockCommand;
import org.maplestar.syrup.commands.internal.CommandManager;
import org.maplestar.syrup.config.Config;
import org.maplestar.syrup.data.DatabaseManager;
import org.maplestar.syrup.data.block.BlockDataManager;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.migration.TakaMigrator;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.listener.ExpGainListener;
import org.maplestar.syrup.listener.LevelChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The entry point of the app.
 */
public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * The entry point of the app.
     *
     * @param args the arguments (assumed to be empty)
     */
    public static void main(String[] args) {
        var config = Config.load();

        var databaseManager = new DatabaseManager(config);
        TakaMigrator.migrateTakaFiles(databaseManager);

        var levelDataManager = new LevelDataManager(databaseManager);
        var blockDataManager = new BlockDataManager(databaseManager);
        var levelRoleDataManager = new LevelRoleDataManager(databaseManager);
        var guildSettingsManager = new GuildSettingsManager(databaseManager);
        var levelChangeListener = new LevelChangeListener(levelRoleDataManager, guildSettingsManager);
        var commandManager = registerCommands(levelDataManager, blockDataManager, levelRoleDataManager, guildSettingsManager, levelChangeListener);

        var jda = JDABuilder.createDefault(config.botToken())
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .setActivity(Activity.playing("NewWorld Online"))
                .addEventListeners(
                        commandManager,
                        new ExpGainListener(levelDataManager, blockDataManager, levelChangeListener)
                )
                .build();

        jda.updateCommands()
                .addCommands(commandManager.getCommandData())
                .queue();

        Runtime.getRuntime().addShutdownHook(new Thread(databaseManager::closeDataSource));

        logger.info("hi!!");
    }

    private static CommandManager registerCommands(LevelDataManager levelDataManager, BlockDataManager blockDataManager, LevelRoleDataManager levelRoleDataManager, GuildSettingsManager guildSettingsManager, LevelChangeListener levelChangeListener) {
        var commandManager = new CommandManager();
        commandManager.registerCommand(new LevelRoleCommand(levelRoleDataManager, guildSettingsManager));
        commandManager.registerCommand(new RankCommand(levelDataManager, levelChangeListener));
        commandManager.registerCommand(new XPBlockCommand(blockDataManager));
        return commandManager;
    }
}
