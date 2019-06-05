package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClearChatCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.clearchat")) {
            return true;
        }
        for (int x = 0; x < 150; x++){
            Bukkit.broadcastMessage("");
        }
        Bukkit.broadcastMessage(Chat.format("&c&l~~ Chat cleared by: " + sender.getName() + " ~~"));
        return true;
    }
}
