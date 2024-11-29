package org.maplestar.syrup.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
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
import org.maplestar.syrup.utils.EmbedMessage;

import java.util.Comparator;
import java.util.List;

/**
 * The /levelrole command for managing levelroles.
 */
public class LevelRoleCommand extends AbstractCommand {
    private final LevelRoleDataManager levelRoleDataManager;
    private final GuildSettingsManager guildSettingsManager;

    /**
     * Initializes the command.
     *
     * @param levelRoleDataManager the level role data manager
     * @param guildSettingsManager the guild settings manager
     */
    public LevelRoleCommand(LevelRoleDataManager levelRoleDataManager, GuildSettingsManager guildSettingsManager) {
        super("levelrole");

        this.levelRoleDataManager = levelRoleDataManager;
        this.guildSettingsManager = guildSettingsManager;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(name, "Manage level roles")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
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

        var subCommandGroup = event.getSubcommandGroup();
        if (subCommandGroup != null && subCommandGroup.equals("settings")) {
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

    /**
     * The /levelrole settings list subcommand.
     * <p>
     * Provides an overview over the current guild's settings.
     *
     * @param event the command event
     */
    private void listSettings(SlashCommandInteractionEvent event) {
        var settings = guildSettingsManager.getSettings(event.getGuild());
        var embedBuilder = new EmbedBuilder()
                .setTitle("**Level role settings**")
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setColor(EmbedColors.primary())
                .addField("**__Remove old roles__**", settings.removeOldRoles() ? "Yes" : "No", true)
                .addField("**__Add roles on rejoin__**", settings.addOnRejoin() ? "Yes" : "No", true);

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    /**
     * The /levelrole settings remove_old_roles subcommand.
     * <p>
     * Can be used to toggle whether old levelroles should be removed when a user gains ones with a higher level.
     *
     * @param event the command event
     */
    private void removeOldRoles(SlashCommandInteractionEvent event) {
        boolean status = event.getOption("status").getAsBoolean();

        var settings = guildSettingsManager.getSettings(event.getGuild()).setRemoveOldRoles(status);

        var success = guildSettingsManager.setSettings(event.getGuild(), settings);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Successfully updated level role settings.")).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to update level role settings.
                    
                    Please contact the bot developer as this is an internal issue.""")).queue();
        }
    }

    /**
     * The /levelrole settings add_on_rejoin subcommand.
     * <p>
     * Can be used to toggle whether old levelroles should be added to a user when they rejoin the guild.
     *
     * @param event the command event
     */
    private void addOnRejoin(SlashCommandInteractionEvent event) {
        boolean status = event.getOption("status").getAsBoolean();

        var settings = guildSettingsManager.getSettings(event.getGuild()).setAddOnRejoin(status);

        var success = guildSettingsManager.setSettings(event.getGuild(), settings);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("Successfully updated level role settings.")).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to update level role settings.
                    
                    Please contact the bot developer as this is an internal issue.""")).queue();
        }
    }

    /**
     * The /levelrole add subcommand.
     * <p>
     * Configures a new levelrole for the current guild, if it's not set up already.
     * The same level may have multiple Discord roles assigned to it.
     *
     * @param event the command event
     */
    private void add(SlashCommandInteractionEvent event) {
        var role = event.getOption("role").getAsRole();
        int level = event.getOption("level").getAsInt();

        var levelRoleOptional = levelRoleDataManager.getLevelRoleData(role, event.getGuild());
        if (levelRoleOptional.isPresent()) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    String.format(
                            "Level role %s already exists (at **Level %d**)",
                            role.getAsMention(),
                            levelRoleOptional.get().level())
            )).queue();
            return;
        }

        var success = levelRoleDataManager.addLevelRole(event.getGuild(), role, level);
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                            Successfully assigned %s to level **%d**.
                            
                            Make sure I have permission to add this role to members. Members who are missing this role will receive it the next time they level up (if applicable)."""
                    .formatted(role.getAsMention(), level))).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to create reaction role.
                    
                    Please contact the bot developer as this is an internal issue.""")).queue();
        }
    }

    /**
     * The /levelrole remove subcommand.
     * <p>
     * Removes a new levelrole from the current guild, if it exists.
     *
     * @param event the command event
     */
    private void remove(SlashCommandInteractionEvent event) {
        var role = event.getOption("role").getAsRole();

        var levelRoleOptional = levelRoleDataManager.getLevelRoleData(role, event.getGuild());
        if (levelRoleOptional.isEmpty()) {
            event.getHook().editOriginalEmbeds(
                    EmbedMessage.error("Level role %s doesn't exist and thus can't be deleted.".formatted(role.getAsMention()))
            ).queue();
            return;
        }

        var success = levelRoleDataManager.removeLevelRole(event.getGuild(), role.getIdLong());
        if (success) {
            event.getHook().editOriginalEmbeds(EmbedMessage.error(
                    String.format(
                            "Successfully removed %s from the list of level roles. It was pointing to level**%d**.",
                            role.getAsMention(),
                            levelRoleOptional.get().level()
                    ))
            ).queue();
        } else {
            event.getHook().editOriginalEmbeds(EmbedMessage.error("""
                    Oops! Failed to delete reaction role.
                    
                    Please contact the bot developer as this is an internal issue.""")).queue();
        }
    }

    /**
     * The /levelrole list subcommand.
     * <p>
     * Provides an overview over all configured roles with their respective levels on the current guild.
     *
     * @param event the command event
     */
    private void listRoles(SlashCommandInteractionEvent event) {
        List<LevelRoleData> levelRoles = levelRoleDataManager.getLevelRoles(event.getGuild()).stream()
                .sorted(Comparator.comparingLong(LevelRoleData::level))
                .toList();

        var embedBuilder = new EmbedBuilder()
                .setTitle("**Level roles**")
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setColor(EmbedColors.primary());
        
        for (var levelRole : levelRoles) {
            embedBuilder.addField("**__Level " + levelRole.level() + "__**", "<@&" + levelRole.roleID() + ">", true);
        }

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    /**
     * The /levelrole cleanup subcommand.
     * <p>
     * Removes all configured levelroles which have since been deleted from the Discord guild from the database.
     *
     * @param event the command event
     */
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
                        event.getHook().editOriginalEmbeds(EmbedMessage.normal("Successfully removed deleted roles from the list.")).queue();
                    } else {
                        event.getHook().editOriginalEmbeds(EmbedMessage.normal("""
                                Oops! Failed to remove deleted roles.
                                
                                Please contact the bot developer as this is an internal issue.""")).queue();
                    }
                });
    }
}
