package org.maplestar.syrup.data.requestdata;

import org.maplestar.syrup.data.reminder.Reminder;

import java.util.List;
import java.util.Map;

public record RequestUserPack(Map<Long, RequestGuildPack> guildPackMap, List<Reminder> reminderList)
{
}
