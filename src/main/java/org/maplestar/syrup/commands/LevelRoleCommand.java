package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.maplestar.syrup.commands.internal.AbstractCommand;
import org.maplestar.syrup.data.levelrole.LevelRoleData;
import org.maplestar.syrup.data.levelrole.LevelRoleDataManager;
import org.maplestar.syrup.data.settings.GuildSettingsManager;
import org.maplestar.syrup.utils.EmbedColors;

import java.util.List;

public class LevelRoleCommand extends AbstractCommand {
    private final LevelRoleDataManager levelRoleDataManager;
    private final GuildSettingsManager guildSettingsManager;

    public LevelRoleCommand(LevelRoleDataManager levelRoleDataManager, GuildSettingsManager guildSettingsManager) {
        super("levelrole");

        this.levelRoleDataManager = levelRoleDataManager;
        this.guildSettingsManager = guildSettingsManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Manage level roles")
                .setGuildOnly(true)
                .addSubcommandGroups(new SubcommandGroupData("settings", "Access the settings for level roles")
                        .addSubcommands(
                                new SubcommandData("list", "View the settings for level roles"),
                                new SubcommandData("add_on_rejoin", "Toggle whether roles should be given back when a user rejoins")
                                        .addOption(OptionType.BOOLEAN, "status", "Is this option on or off?", true),
                                new SubcommandData("remove_old_roles", "If a user gains a new level role, remove the old ones!!")
                                        .addOption(OptionType.BOOLEAN, "status", "Is this option on or off?", true)
                        ))
                .addSubcommands(
                        new SubcommandData("add", "Adds a new level role")
                                .addOption(OptionType.ROLE, "role", "The role associated with this level role", true)
                                .addOption(OptionType.INTEGER, "level", "The level at which you obtain this role", true),
                        new SubcommandData("remove", "Removes a level role")
                                .addOption(OptionType.ROLE, "role", "The level role which should be deleted", true),
                        new SubcommandData("list", "Lists all currently configured level roles"),
                        new SubcommandData("cleanup", "If any deleted roles still exist in this list, they get whacked away!!")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (event.getSubcommandGroup().equals("settings")) {
            switch (event.getSubcommandName()) {
                case "list" -> listSettings(event);
                case "add_on_rejoin" -> addOnRejoin(event);
                case "remove_old_roles" -> removeOldRoles(event);
                case null, default -> throw new IllegalArgumentException();
            }
        } else {
            switch (event.getSubcommandName()) {
                case "add" -> add(event);
                case "remove" -> remove(event);
                case "list" -> listRoles(event);
                case "cleanup" -> cleanup(event);
                case null, default -> throw new IllegalArgumentException();
            }
        }
    }

    private void listSettings(SlashCommandInteractionEvent event) {
        var settings = guildSettingsManager.getSettings(event.getGuild());
        var embedBuilder = new EmbedBuilder()
                .setTitle("**Level role settings")
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setColor(EmbedColors.primary())
                .addField("**__Remove old roles__**", settings.removeOldRoles() ? "Yes" : "No", true)
                .addField("**__Add roles on rejoin__**", settings.addOnRejoin() ? "Yes" : "No", true);

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void removeOldRoles(SlashCommandInteractionEvent event) {
        boolean status = event.getOption("status").getAsBoolean();

        var settings = guildSettingsManager.getSettings(event.getGuild()).setRemoveOldRoles(status);
        var success = guildSettingsManager.setSettings(event.getGuild(), settings); // TODO: Do something with the success message

        event.getHook().editOriginal("Successfully updated level role settings.").queue();
    }

    private void addOnRejoin(SlashCommandInteractionEvent event) {
        boolean status = event.getOption("status").getAsBoolean();

        var settings = guildSettingsManager.getSettings(event.getGuild()).setAddOnRejoin(status);
        var success = guildSettingsManager.setSettings(event.getGuild(), settings); // TODO: Do something with the success message

        event.getHook().editOriginal("Successfully updated level role settings.").queue();
    }

    private void add(SlashCommandInteractionEvent event) {
        var role = event.getOption("role").getAsRole();
        int level = event.getOption("level").getAsInt();

        var roles = levelRoleDataManager.getLevelRoles(event.getGuild());

        // if role already exists, cancels function
        for (var levelRole : roles)
        {
            if(levelRole.roleID() != role.getIdLong()) continue;

            event.getHook().editOriginal("Level role <@&" + role.getId() + "> already exists " +
                    "(at **Level " + levelRole.level() + "**)")
                    .queue();
            return;
        }

        levelRoleDataManager.addLevelRole(event.getGuild(), role, level);

        event.getHook().editOriginal(
                "Successfully assigned <@&" + role.getId() + "> to level **" + level + "**. Make sure I have" +
                "permission to add this role to members. Members who are missing this role will receive it the next" +
                "time they level up (if applicable).")
                .queue();
    }

    private void remove(SlashCommandInteractionEvent event) {
        var role = event.getOption("role").getAsRole();


        var tryGetLevel = levelRoleDataManager.getLevel(role, event.getGuild());
        if(!tryGetLevel.isPresent())
        {
            event.getHook().editOriginal("Level role <@&" + role.getId() + "> doesn't exist and thus can't be deleted.")
                    .queue();
            return;
        }
        int level = tryGetLevel.get();

        levelRoleDataManager.removeLevelRole(event.getGuild(), role.getIdLong());

        event.getHook().editOriginal("Successfully removed <@&" + role.getId() + "> from the list of level roles. It was pointing to level" + "**" + level + "**.")
                .queue();
    }

    private void listRoles(SlashCommandInteractionEvent event) {
        List<LevelRoleData> levelRoles = levelRoleDataManager.getLevelRoles(event.getGuild());

        var embedBuilder = new EmbedBuilder()
                .setTitle("**Level role settings")
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setColor(EmbedColors.primary());

        for (var levelRole : levelRoles) {
            embedBuilder.addField("**__Level " + levelRole.level() + "__**", "<@&" + levelRole.roleID() + ">", true);
        }

        var embed = embedBuilder.build();
        event.getHook().editOriginalEmbeds(embed).queue();
    }

    private void cleanup(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        var guildRoles = guild.getRoles().stream()
                .map(Role::getIdLong)
                .toList();

        levelRoleDataManager.getLevelRoles(guild).stream()
                .map(LevelRoleData::roleID)
                .filter(levelRoleID -> !guildRoles.contains(levelRoleID))
                .forEach(invalidLevelRoleID -> {
                    var success = levelRoleDataManager.removeLevelRole(guild, invalidLevelRoleID);
                    if (success) {
                        event.getHook().editOriginal("Successfully removed deleted roles from the list.").queue();
                    } else {
                        event.getHook().editOriginal("Oops! Failed to remove deleted roles. Please contact the bot developer as this is an internal issue.").queue();
                    }
                });
    }
}
