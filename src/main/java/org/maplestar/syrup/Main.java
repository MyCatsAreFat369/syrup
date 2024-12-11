package org.maplestar.syrup;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.maplestar.syrup.commands.*;
import org.maplestar.syrup.commands.internal.CommandManager;
import org.maplestar.syrup.config.Config;
import org.maplestar.syrup.data.DatabaseManager;
import org.maplestar.syrup.data.block.BlockDataManager;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.migration.TakaMigrator;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.executors.ReminderExecutor;
import org.maplestar.syrup.listener.ExpGainListener;
import org.maplestar.syrup.listener.GuildMemberJoinListener;
import org.maplestar.syrup.listener.LevelChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

/**
 * The entry point of the app.
 */
public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    private static LevelDataManager levelDataManager;
    private static BlockDataManager blockDataManager;
    private static LevelRoleDataManager levelRoleDataManager;
    private static GuildSettingsManager guildSettingsManager;
    private static LevelChangeListener levelChangeListener;
    private static ReminderDataManager reminderDataManager;

    /**
     * The entry point of the app.
     *
     * @param args the arguments (assumed to be empty)
     */
    public static void main(String[] args) {
        loadFonts();

        var config = Config.load();

        var databaseManager = new DatabaseManager(config);
        TakaMigrator.migrateTakaFiles(databaseManager);

        levelDataManager = new LevelDataManager(databaseManager);
        blockDataManager = new BlockDataManager(databaseManager);
        levelRoleDataManager = new LevelRoleDataManager(databaseManager);
        guildSettingsManager = new GuildSettingsManager(databaseManager);
        reminderDataManager = new ReminderDataManager(databaseManager);
        levelChangeListener = new LevelChangeListener(levelRoleDataManager, guildSettingsManager);
        var commandManager = registerCommands();

        var jda = JDABuilder.createDefault(config.botToken())
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("NewWorld Online"))
                .addEventListeners(
                        commandManager,
                        new ExpGainListener(levelDataManager, blockDataManager, levelChangeListener),
                        new GuildMemberJoinListener(guildSettingsManager, levelDataManager, levelChangeListener)
                )
                .build();

        jda.updateCommands()
                .addCommands(commandManager.getCommandData())
                .queue();

        Runtime.getRuntime().addShutdownHook(new Thread(databaseManager::closeDataSource));

        var reminderExecutor = new ReminderExecutor(jda, reminderDataManager);
        reminderExecutor.init();

        logger.info("hi!!");
    }

    /**
     * Registers the bot's commands with the {@link CommandManager} and subsequently JDA.
     *
     * @return the configured command manager
     */
    private static CommandManager registerCommands() {
        var commandManager = new CommandManager();
        commandManager.registerCommand(new EditRankCommand(levelDataManager, levelChangeListener));
        commandManager.registerCommand(new LeaderboardCommand(levelDataManager));
        commandManager.registerCommand(new LevelRoleCommand(levelRoleDataManager, guildSettingsManager));
        commandManager.registerCommand(new RankCommand(levelDataManager));
        commandManager.registerCommand(new RemindMeCommand(reminderDataManager));
        commandManager.registerCommand(new RemindMeNukeCommand(reminderDataManager));
        commandManager.registerCommand(new XPBlockCommand(blockDataManager));
        return commandManager;
    }

    /**
     * Loads the fonts required to draw this bot's images from the application's resources.
     */
    private static void loadFonts() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            var url = Main.class.getResource("/fonts/KiwiMaru-Regular.ttf");
            var inputStream = url.openStream();
            graphicsEnvironment.registerFont(Font.createFont(Font.TRUETYPE_FONT, inputStream));
            logger.info("Registered font");
        } catch (FontFormatException | IOException | NullPointerException exception) {
            logger.warn("Oops, KiwiMaru could not be registered", exception);
        }
    }
}
