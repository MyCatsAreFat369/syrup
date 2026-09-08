package org.maplestar.syrup.listener;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.checkbox.Checkbox;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.maplestar.syrup.config.Config;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.data.requestdata.RequestGuildPack;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;
import org.maplestar.syrup.utils.EmbedMessage;
import org.maplestar.syrup.utils.RequestDataUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ButtonListener extends ListenerAdapter
{
    private final LevelDataManager levelDataManager;
    private final XPBlockDataManager xpBlockDataManager;
    private final ReminderDataManager reminderDataManager;
    private final Config config;

    public ButtonListener(LevelDataManager levelDataManager, XPBlockDataManager xpBlockDataManager,
                          ReminderDataManager reminderDataManager, Config config)
    {
        this.levelDataManager = levelDataManager;
        this.xpBlockDataManager = xpBlockDataManager;
        this.reminderDataManager = reminderDataManager;
        this.config = config;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        if(event.getComponentId().startsWith("requestdeletionbutton"))
        {
            String userIDStr = event.getComponentId().split(";")[1];
            long userID = Long.parseLong(userIDStr);

            List<SelectOption> guildOptions = new ArrayList<>();

            var requestUserPack = RequestDataUtils.getDataRequestPack(userID, levelDataManager, xpBlockDataManager, reminderDataManager);
            var guildPackMap = requestUserPack.guildPackMap();

            for(Map.Entry<Long, RequestGuildPack> entry : guildPackMap.entrySet())
            {
                long guildID = entry.getKey();
                var requestGuildPack = entry.getValue();
                var levelData = requestGuildPack.levelData();
                var guild = event.getJDA().getGuildById(guildID);

                var selectOption = SelectOption.of(String.format("%s (%d)", guild == null ? "Unknown" : guild.getName(), guildID),
                        Long.toString(guildID))
                        .withDescription(String.format("Level: %d, XP: %d", levelData.level(), levelData.xp()));
                guildOptions.add(selectOption);
            }

            Modal modal = Modal.create("requestdeletionmodal;" + userID, "Request Data Deletion Form")
                    .addComponents(
                            TextDisplay.of(
                                    "Welcome to the Data Deletion modal! Here, you can delete any data which " +
                                    "Syrup's Privacy Policy allows you to delete. For more information, visit " +
                                    config.privacyPolicyURL()),
                            Label.of("Level Data Deletion",
                                    StringSelectMenu.create("leveldatadeletionmenu")
                                            .addOptions(guildOptions)
                                            .setMinValues(1)
                                            .setMaxValues(guildOptions.size())
                                            .build())
                    )
                    .build();

            event.replyModal(modal).queue();
        }
    }
}
