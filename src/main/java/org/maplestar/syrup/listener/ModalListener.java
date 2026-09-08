package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.maplestar.syrup.config.Config;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;
import org.maplestar.syrup.utils.EmbedMessage;

import java.util.ArrayList;
import java.util.List;

public class ModalListener extends ListenerAdapter
{
    private final LevelDataManager levelDataManager;

    public ModalListener(LevelDataManager levelDataManager)
    {
        this.levelDataManager = levelDataManager;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event)
    {
        if(event.getModalId().startsWith("requestdeletionmodal"))
        {
            String userIDStr = event.getModalId().split(";")[1];
            long userID = Long.parseLong(userIDStr);

            var levelDataDeletionGuilds = event.getValue("leveldatadeletionmenu").getAsStringList();
            boolean[] successes = new boolean[levelDataDeletionGuilds.size()];
            for(int i = 0; i < levelDataDeletionGuilds.size(); i++)
            {
                long guildID = Long.parseLong(levelDataDeletionGuilds.get(i));
                successes[i] = levelDataManager.deleteLevelDataAtRequest(guildID, userID);
            }

            List<String> failedDeletions = new ArrayList<>();
            for(int i = 0; i < successes.length; i++)
            {
                if(!successes[i])
                {
                    failedDeletions.add(levelDataDeletionGuilds.get(i));
                }
            }

            StringBuilder response = new StringBuilder();

            if(failedDeletions.isEmpty())
            {
                response.append("Successfully deleted the specified data for the following guilds:\n\n");
                for(String guildID : levelDataDeletionGuilds)
                {
                    var guild = event.getJDA().getGuildById(Long.parseLong(guildID));
                    response.append(String.format("- %s (%s)\n", guild == null ? "Unknown" : guild.getName(), guildID));
                }
                response.append("The data above cannot be recovered, unless a staff member may vouch for you or logs can be found.");
            } else if(failedDeletions.size() < levelDataDeletionGuilds.size())
            {
                response.append("We deleted the specified data for some guilds, but due to an error we couldn't delete the data for the following guilds:\n");
                for(String guildID : failedDeletions)
                {
                    var guild = event.getJDA().getGuildById(Long.parseLong(guildID));
                    response.append(String.format("- %s (%s)\n", guild == null ? "Unknown" : guild.getName(), guildID));
                }
                response.append("Please reach out to one of our bot developers to proceed with the deletion of the remaining data. " +
                        "You can find our contact information in Syrup's bio!");
            } else
            {
                response.append("We couldn't delete any of the specified data, due to an error with your request. Please " +
                        "reach out to one of our bot developers to proceed with the deletion of the remaining data. " +
                        "You can find our contact information in Syrup's bio!");
            }

            event.replyEmbeds(EmbedMessage.normal(response.toString())).setEphemeral(true).queue();
        }
    }
}
