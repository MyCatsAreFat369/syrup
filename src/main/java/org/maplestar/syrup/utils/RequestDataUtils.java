package org.maplestar.syrup.utils;

import org.maplestar.syrup.data.requestdata.RequestGuildPack;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.reminder.Reminder;
import org.maplestar.syrup.data.reminder.ReminderDataManager;
import org.maplestar.syrup.data.requestdata.RequestUserPack;
import org.maplestar.syrup.data.xpblock.XPBlockData;
import org.maplestar.syrup.data.xpblock.XPBlockDataManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestDataUtils
{
    public static RequestUserPack getDataRequestPack(long userID, LevelDataManager levelDataManager,
                                                     XPBlockDataManager xpBlockDataManager, ReminderDataManager reminderDataManager)
    {
        /// Data stored
        List<LevelData> levelDataList = levelDataManager.getLevelDataForDataRequest(userID);
        List<XPBlockData> xpBlockList = xpBlockDataManager.getXPBlockForDataRequest(userID);
        List<Reminder> reminderList = reminderDataManager.getRemindersForDataRequest(userID);

        Map<Long, RequestGuildPack> guildPackMap = new HashMap<>();

        for(var levelData : levelDataList)
        {
            if(!guildPackMap.containsKey(levelData.guildID()))
            {
                guildPackMap.put(levelData.guildID(), new RequestGuildPack(null, null));
            }
            var requestDataPack = guildPackMap.get(levelData.guildID());
            guildPackMap.replace(levelData.guildID(), requestDataPack.setLevelData(levelData));
        }

        for(var xpBlockData : xpBlockList)
        {
            if(!guildPackMap.containsKey(xpBlockData.guildID()))
            {
                guildPackMap.put(xpBlockData.guildID(), new RequestGuildPack(null, null));
            }
            var requestDataPack = guildPackMap.get(xpBlockData.guildID());
            guildPackMap.replace(xpBlockData.guildID(), requestDataPack.setXPBlockData(xpBlockData));
        }

        return new RequestUserPack(guildPackMap, reminderList);
    }
}
