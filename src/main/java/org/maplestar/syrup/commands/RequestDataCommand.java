package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.AttachedFile;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.config.Config;
import org.maplestar.syrup.data.requestdata.RequestGuildPack;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.reminder.Reminder;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;
import org.maplestar.syrup.utils.EmbedMessage;
import org.maplestar.syrup.utils.RequestDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RequestDataCommand extends AbstractCommand
{
    private final Logger logger = LoggerFactory.getLogger(RequestDataCommand.class);
    private final LevelDataManager levelDataManager;
    private final ReminderDataManager reminderDataManager;
    private final XPBlockDataManager xpBlockDataManager;
    private final Config config;

    public RequestDataCommand(LevelDataManager levelDataManager, XPBlockDataManager xpBlockDataManager,
                              ReminderDataManager reminderDataManager, Config config)
    {
        super("requestdata");

        this.levelDataManager = levelDataManager;
        this.reminderDataManager = reminderDataManager;
        this.xpBlockDataManager = xpBlockDataManager;
        this.config = config;
    }

    @Override
    public SlashCommandData getSlashCommandData()
    {
        return Commands.slash(name, "Request your data and learn how we use it.");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        var user = event.getUser();

        long userID = user.getIdLong();
        var requestUserPack = RequestDataUtils.getDataRequestPack(userID, levelDataManager, xpBlockDataManager, reminderDataManager);
        var guildPackMap = requestUserPack.guildPackMap();
        var reminderList = requestUserPack.reminderList();

        try
        {
            File f = new File(String.format("temp/%s.zip", user.getId()));
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
            /// All guild .zip files
            for(Map.Entry<Long, RequestGuildPack> entry : guildPackMap.entrySet())
            {
                long guildID = entry.getKey();
                RequestGuildPack requestGuildPack = entry.getValue();
                ZipEntry e = new ZipEntry(String.format("%s.txt", guildID));
                out.putNextEntry(e);

                String strContents = createDatapackPerGuild(event.getJDA(), guildID, requestGuildPack);
                byte[] data = strContents.getBytes(StandardCharsets.UTF_8);
                out.write(data, 0, data.length);
                out.closeEntry();
            }
            /// All reminders in a .txt file
            ZipEntry e = new ZipEntry("reminders.txt");
            out.putNextEntry(e);

            String strContents = createReminderPack(userID, reminderList);
            byte[] data = strContents.getBytes(StandardCharsets.UTF_8);
            out.write(data, 0, data.length);
            out.closeEntry();

            /// README file creation (.txt)
            e = new ZipEntry("README.txt");
            out.putNextEntry(e);

            strContents = createReadMe(userID);
            byte[] data2 = strContents.getBytes(StandardCharsets.UTF_8);
            out.write(data2, 0, data2.length);
            out.closeEntry();

            out.close();

            event.getHook().editOriginalAttachments(AttachedFile.fromData(f, "syrupDataRequest.zip"))
                    .setComponents(ActionRow.of(Button.primary("requestdeletionbutton;" + userID, "Delete Deletable Data"))).queue(
                            success -> {
                                deleteTempFile(f);
                            },
                            failure -> {
                                logger.error("Couldn't send data request pack to user", failure);
                                deleteTempFile(f);
                            }
                    );
        } catch(IOException exception)
        {
            logger.error("Couldn't create data request pack", exception);
            event.getHook().editOriginalEmbeds(EmbedMessage.error("Couldn't create data request pack. Please reach out to the bot developers, " +
                    "you may contact them either through discord or through Syrup's mail system.")).queue();
        }
    }

    private String createDatapackPerGuild(JDA jda, long guildID, RequestGuildPack requestGuildPack)
    {
        var guild = jda.getGuildById(guildID);
        var levelData = requestGuildPack.levelData();
        var xpBlockData = requestGuildPack.xpBlockData();

        StringBuilder dataStr = new StringBuilder();

        dataStr.append("Guild-Specific Data\n");
        dataStr.append("===============================================================\n\n");

        dataStr.append("Guild Name:\n");
        dataStr.append(guild == null ? "Unknown" : guild.getName());
        dataStr.append("\nGuild ID:\n");
        dataStr.append(guildID);
        dataStr.append("\n\n");

        dataStr.append("""
                This report shows guild-specific information stored by Syrup that is associated with your Discord account. Sections may
                also explain data that could be stored even when no corresponding record currently exists.""");
        dataStr.append("\n\n");

        dataStr.append("LEVEL DATA\n");
        dataStr.append("------------\n\n");

        if(levelData == null)
        {
            dataStr.append("You don't have any level data for this guild, or we couldn't query any such results.\n");
            dataStr.append("If you believe this is an error, please reach out to the staff team or developer of the bot\n");
            dataStr.append("to get your data, or simply try again later.\n\n");
        }
        else
        {
            dataStr.append("Level:\n");
            dataStr.append(levelData.level());
            dataStr.append("\n\n");

            dataStr.append("XP:\n");
            dataStr.append(levelData.xp());
            dataStr.append("\n\n");
        }

        dataStr.append("Purpose of this data:\n");
        dataStr.append("""
                This information is used to associate your Discord account with your guild-specific leveling data,
                allowing the bot to keep track of how active you've been in the server, and to allow server moderators
                to assign Level Roles to your account, helping give privileges to user accounts who have proven they aren't
                suspicious or troll accounts.
                
                Your Discord User ID and the corresponding Guild ID are used to maintain this association.""");

        dataStr.append("\n\n");
        dataStr.append("XP BLOCKLIST DATA\n");
        dataStr.append("-----------------\n\n");

        if(xpBlockData == null)
        {
            dataStr.append("You don't have any xp block data for this guild, or we couldn't query any such results.\n");
            dataStr.append("If you believe this is an error, please reach out to the staff team or developer of the bot\n");
            dataStr.append("to get your data, or simply try again later.\n\n");
        }
        else
        {
            dataStr.append("You are blocked from receiving XP in this guild.\n\n");
            dataStr.append("Time of block (UTC+0):\n");
            dataStr.append(Instant.ofEpochMilli(xpBlockData.timeInMillis()).atZone(ZoneId.of("UTC")));
            dataStr.append("\nIn epoch seconds: ");
            dataStr.append(xpBlockData.timeInMillis() / 1000L);
            dataStr.append("\n\n");
        }

        dataStr.append("Purpose of this data:\n");
        dataStr.append("""
                If blocked from receiving XP in a certain guild, your Discord User ID is retained so that you may not
                receive XP in that guild and the bot can continue enforcing restriction on XP gain.
                
                The time at which you got blocked is kept for moderation and logging purposes.
                
                You may not delete your xp block data as that would render the xp block restriction
                meaningless, and would pose an issue for the server's moderation.""");

        dataStr.append("\n\n");
        dataStr.append("OTHER INFORMATION\n");
        dataStr.append("-----------------\n\n");

        dataStr.append("""
                Only the level data is deletable, as the other data is vital for the functionality of the guild's
                moderation.""");

        dataStr.append("\n\n");
        dataStr.append("DATA RETENTION\n");
        dataStr.append("--------------\n\n");

        dataStr.append("Level Data:\n");
        dataStr.append("Retained until you request deletion.\n\n");

        dataStr.append("XP Block Data:\n");
        dataStr.append("Retained while the corresponding restriction remains in effect.\n\n");

        dataStr.append("===============================================================\n\n");

        dataStr.append("You may view our Privacy Policy in full detail by visiting the following link:\n");
        dataStr.append(config.privacyPolicyURL());

        return dataStr.toString();
    }

    private String createReminderPack(long userID, List<Reminder> reminderList)
    {
        StringBuilder dataStr = new StringBuilder();

        dataStr.append("User-specific Reminder Data\n");
        dataStr.append("===============================================================\n\n");

        dataStr.append("User ID:\n");
        dataStr.append(userID);
        dataStr.append("\n\n");

        dataStr.append("""
                This report shows user-specific information stored by Syrup that is associated with your Discord account.
                It contains an exhaustive list of all the Syrup reminders you've created thus far which haven't fired yet.
                Whenever a reminder fires (reminds you), it gets removed from our databases.""");
        dataStr.append("\n\n");

        dataStr.append("REMINDER DATA\n");
        dataStr.append("------------\n\n");

        if(reminderList.isEmpty())
        {
            dataStr.append("You don't have any reminder data associated with your account, or we couldn't query any such results.\n");
            dataStr.append("If you believe this is an error, please reach out to the staff team or developer of the bot\n");
            dataStr.append("to get your data, or simply try again later.\n\n");
        }
        else
        {
            for(var reminder : reminderList)
            {
                dataStr.append("Reminder ID:\n");
                dataStr.append(reminder.id());
                dataStr.append("\n\n");

                dataStr.append("Time (UTC+0):\n");
                dataStr.append(Instant.ofEpochMilli(reminder.timeInMillis()).atZone(ZoneId.of("UTC")));
                dataStr.append("\nIn epoch seconds: ");
                dataStr.append(reminder.timeInSeconds() / 1000L);
                dataStr.append("\n\n");

                dataStr.append("Reminder Message:\n");
                dataStr.append(reminder.message());
                dataStr.append("\n\n");

                dataStr.append("Reminder Channel ID:\n");
                dataStr.append(reminder.channelID());
                dataStr.append("\n\n");
            }
        }

        dataStr.append("Purpose of this data:\n");
        dataStr.append("""
                This information is used to keep track of the reminders you've created with your Discord account in the past,
                allowing the bot to remind you punctually, and you may opt out at any time. To opt out, simply delete all
                of your reminder data using the command `/reminder nuke`, and then afterwards it's your choice whether or
                not to create new reminders.
                
                Your Discord User ID and the corresponding Channel ID are used to maintain this association. The Channel ID
                stems from the channel in which the reminder was created. It could be our bot DMs too!""");

        dataStr.append("\n\n");
        dataStr.append("OTHER INFORMATION\n");
        dataStr.append("-----------------\n\n");

        dataStr.append("""
                All of your reminder data is deletable, and may be deleted by running `/reminder nuke`. Note: this will
                delete all of your reminders, so be careful when running this command!
                
                You may also delete individual reminders, so fret not! Simply run `/reminder remove <id>` to remove
                a specific reminder.""");

        dataStr.append("\n\n");
        dataStr.append("DATA RETENTION\n");
        dataStr.append("--------------\n\n");

        dataStr.append("Your reminder data is retained until a reminder fires (reminds you) or you delete it.\n\n");

        dataStr.append("===============================================================\n\n");

        dataStr.append("You may view our Privacy Policy in full detail by visiting the following link:\n");
        dataStr.append(config.privacyPolicyURL());

        return dataStr.toString();
    }

    private String createReadMe(long userID)
    {
        StringBuilder dataStr = new StringBuilder();

        dataStr.append("README for Data Request Pack\n");
        dataStr.append("============================\n\n");

        dataStr.append("Discord User ID:\n");
        dataStr.append(userID);
        dataStr.append("\n\n");

        dataStr.append("""
                This data pack contains all of the data that Syrup stores about you. It contains:
                - Level Data contained per guild,
                - XP Block Data contained per guild, and
                - Reminder Data connected to your Discord account
                
                The data that is deletable is:
                - Level Data
                - Reminder Data
                
                Other data is deemed as vital to the bot and each server's functionality, and thus can't be deleted.
                
                Your level data may be deleted by simply running the `/requestdata` command and clicking on the
                `Delete Deletable Data` button. On the other hand, Reminder Data has its own commands for deletion,
                namely `/reminder remove <id>` and `/reminder nuke`, for deleting one or all reminders respectively.
                This is explained in the various text files in this data pack.
                
                The data pack contains a series of text files, each named by their corresponding Guild ID (for general
                data), or reminders.txt for your reminder data. The file you're reading now is README.txt, which displays
                general information for how the data pack is organized.
                
                """);

        dataStr.append("For more information, you may view our Privacy Policy in full detail by visiting the following link:\n");
        dataStr.append(config.privacyPolicyURL());

        return dataStr.toString();
    }

    private void deleteTempFile(File f)
    {
        try
        {
            if(f.delete())
            {
                logger.info("Successfully deleted temporary data request pack");
            } else
            {
                logger.info("Couldn't delete temporary data request pack.");
            }
        } catch(SecurityException exception)
        {
            logger.error("Couldn't delete temporary data request pack", exception);
        }
    }
}
