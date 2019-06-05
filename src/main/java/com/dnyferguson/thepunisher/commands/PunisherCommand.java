package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

public class PunisherCommand implements CommandExecutor {

    private String headerFormat;
    private String listItemFormat;
    private String noAvailableCommands;

    public PunisherCommand(ThePunisher plugin) {
        ConfigurationSection messages = plugin.getConfig().createSection("messages");
        this.headerFormat = messages.getString("punisher-command-header");
        this.listItemFormat = messages.getString("punisher-command-list-format");
        this.noAvailableCommands = messages.getString("punisher-command-no-available-commands");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.list")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return false;
        }

        int availableCommands = 0;

        StringBuilder message = new StringBuilder();
        message.append(headerFormat);

        if (sender.hasPermission("punisher.ban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/ban (username/uuid/ip) (time [optional]) (reason)").replace("%usage%", "Permanently ban a player."));
        }

        if (sender.hasPermission("punisher.unban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unban (username/uuid/ip)").replace("%usage%", "Unban a player."));
        }

        if (sender.hasPermission("punisher.checkban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/checkban (username/uuid/ip)").replace("%usage%", "Check if a player is currently banned."));
        }

        if (sender.hasPermission("punisher.mute")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/mute (username/uuid/ip) (time [optional]) (reason)").replace("%usage%", "Mute a player."));
        }

        if (sender.hasPermission("punisher.unmute")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unmute (username/uuid/ip)").replace("%usage%", "Unmute a player."));
        }

        if (sender.hasPermission("punisher.checkmute")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/checkmute (username/uuid/ip)").replace("%usage%", "Check if a player is currently muted."));
        }

        if (sender.hasPermission("punisher.alts")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/alts (username/uuid/ip)").replace("%usage%", "Check whos sharing an ip with a player."));
        }

        if (sender.hasPermission("punisher.kick")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/kick (username) (reason)").replace("%usage%", "Kick a player."));
        }

        if (sender.hasPermission("punisher.bypassban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/bypassban (username/uuid)").replace("%usage%", "Allow a specific player to bypass a ban."));
        }

        if (sender.hasPermission("punisher.bypassmute")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/bypassmute (username/uuid)").replace("%usage%", "Allow a specific player to bypass a mute."));
        }

        if (sender.hasPermission("punisher.unbypassban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unbypassban (username/uuid)").replace("%usage%", "Remove a player from the bypass bans list."));
        }

        if (sender.hasPermission("punisher.unbypassmute")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unbypassmute (username/uuid)").replace("%usage%", "Remove a player from the bypass mutes list."));
        }

        if (sender.hasPermission("punisher.iphistory")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/iphistory (username/uuid)").replace("%usage%", "Check a player\'s ip history."));
        }

        if (sender.hasPermission("punisher.warn")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/warn (username/uuid) (reason)").replace("%usage%", "Issue a warning to a player."));
        }

        if (sender.hasPermission("punisher.staffrollback")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/staffrollback (username/uuid) (bans/warns/mutes/kicks/all) (time)").replace("%usage%", "Roll back a staff member\'s actions."));
        }

        if (sender.hasPermission("punisher.unwarn")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unwarn (username/uuid)").replace("%usage%", "Removes a players last warn."));
        }

        if (sender.hasPermission("punisher.history")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/history (username/uuid) (limit [optional])").replace("%usage%", "View a players recent punishments."));
        }

        if (sender.hasPermission("punisher.clearchat")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/clearchat").replace("%usage%", "Clear chat."));
        }

        // If has access to 0 commands, return message stating so
        if (availableCommands < 1) {
            message.append("\n");
            message.append(noAvailableCommands);
        }

        sender.sendMessage(Chat.format(message.toString().replace("%amount%", "" + availableCommands)));
        return true;
    }
}
