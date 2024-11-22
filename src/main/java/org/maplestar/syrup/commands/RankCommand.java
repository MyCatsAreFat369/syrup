package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.rank.LevelData;
import org.maplestar.syrup.data.rank.LevelDataManager;
import org.maplestar.syrup.data.rank.RankingData;
import org.maplestar.syrup.listener.LevelChangeListener;
import org.maplestar.syrup.listener.event.LevelChangeEvent;
import org.maplestar.syrup.utils.EmbedColors;
import org.maplestar.syrup.utils.UserUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RankCommand extends AbstractCommand {
    private final LevelDataManager levelDataManager;
    private final ExecutorService executorService;
    private final LevelChangeListener levelChangeListener;

    public RankCommand(LevelDataManager levelDataManager, LevelChangeListener levelChangeListener) {
        super("rank");

        this.levelDataManager = levelDataManager;
        this.levelChangeListener = levelChangeListener;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Display your rank")
                .setGuildOnly(true)
                .addSubcommands(
                        new SubcommandData("user", "Display your rank!")
                                .addOption(OptionType.USER, "user", "The user", false),
                        new SubcommandData("leaderboard", "Display leaderboard!")
                                .addOption(OptionType.INTEGER, "page", "The page", false),
                        new SubcommandData("edit", "Edit a user's rank")
                                .addOption(OptionType.USER, "user", "The user", true)
                                .addOptions(new OptionData(OptionType.STRING, "type", "The type", true)
                                        .addChoice("XP", "XP")
                                        .addChoice("Level", "LEVEL")
                                )
                                .addOption(OptionType.INTEGER, "value", "The new value", true)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getSubcommandName().equals("user")) {
            user(event);
        } else if (event.getSubcommandName().equals("leaderboard")) {
            leaderboard(event);
        } else {
            edit(event);
        }
    }

    private void user(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        var user = event.getOption("user", event.getUser(), OptionMapping::getAsUser);
        var levelData = levelDataManager.getLevelData(user, event.getGuild());

        event.getHook().editOriginal(
                "<@" + user.getId() + ">'s Level Stats:\n" +
                "Level " + levelData.level() + "\n" +
                levelData.xp() + " XP")
                .queue();
    }

    private void leaderboard(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        executorService.submit(() -> {
            int page = event.getOption("page", 1, OptionMapping::getAsInt);
            var guild = event.getGuild();
            var rankedUsers = levelDataManager.getTopUsers(guild, page);
            var userRank = levelDataManager.getRankingData(event.getUser(), guild);

            var embedBuilder = new EmbedBuilder()
                    .setTitle(":bar_chart: XP Leaderboard for " + guild.getName())
                    .setColor(EmbedColors.primary());

            String desc = String.format(
                    "<@%d>, you currently have **%,d** XP (Level **%d**) and are in position **%,d**",
                    event.getUser().getIdLong(),
                    userRank.levelData().xp(),
                    userRank.levelData().level(),
                    userRank.rank()
            );

            desc += "\n```";
            for (int i = 0; i < rankedUsers.size(); i++) {
                RankingData rankingData = rankedUsers.get(i);
                String rankStr = String.format("%.6s", rankingData.rank());
                rankStr += " ".repeat(6 - rankStr.length());
                desc += rankStr + " | ";

                String nameStr = UserUtils.getNameByUserID(guild, rankingData.userID());
                if (nameStr.length() == 18) {
                    nameStr = nameStr.substring(0, 17);
                    nameStr += "…";
                }// try now
                nameStr += " ".repeat(18 - nameStr.length());

                desc += nameStr + " | ";

                // Level <level> starts with 6 characters plus the extra
                int levelStrLength = 6 + ("" + rankedUsers.get(0).levelData().level()).length();
                String levelStr = "Level " + rankingData.levelData().level();
                levelStr += " ".repeat(levelStrLength - levelStr.length());
                desc += levelStr + " | ";

                String xpStr = String.format("%,d", rankingData.levelData().xp()) + " XP";
                desc += xpStr;

                if (i + 1 < rankedUsers.size()) desc += "\n";
            }

            desc += "```";

            // ok we're done
            // mazing
            // heh

            embedBuilder.setDescription(desc);

            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        });
    }

    private void edit(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        var user = event.getOption("user").getAsUser();
        var type = RankCommandType.valueOf(event.getOption("type").getAsString());
        var value = event.getOption("value").getAsInt();

        var oldLevelData = levelDataManager.getLevelData(user, event.getGuild());
        LevelData newLevelData = null;

        if (type == RankCommandType.XP) {
            newLevelData = oldLevelData.setXP(value);
        } else if (type == RankCommandType.LEVEL) {
            newLevelData = oldLevelData.setLevel(value);
        }

        if (newLevelData.level() != oldLevelData.level()) {
            levelChangeListener.onLevelChange(new LevelChangeEvent(event.getGuild(), user, newLevelData, oldLevelData));
        }

        levelDataManager.setLevelData(user, event.getGuild(), newLevelData);

        event.getHook().editOriginal("Set user <@" + user.getId() + ">'s " + type.toString().toLowerCase() + " to " + value).queue();
    }

    private enum RankCommandType {
        LEVEL, XP
    }
}