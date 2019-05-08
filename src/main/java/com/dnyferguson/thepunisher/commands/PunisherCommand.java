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
        int availableCommands = 0;

        StringBuilder message = new StringBuilder();
        message.append(headerFormat);

        if (sender.hasPermission("punisher.ban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/ban (username/uuid/ip) (reason)").replace("%usage%", "Permanently ban a user."));
        }

        if (sender.hasPermission("punisher.unban")) {
            availableCommands++;
            message.append("\n");
            message.append(listItemFormat.replace("%command%", "/unban (username/uuid/ip)").replace("%usage%", "Unban a user."));
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
