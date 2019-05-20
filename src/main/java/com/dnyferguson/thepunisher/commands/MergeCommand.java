package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MergeCommand implements CommandExecutor {

    private ThePunisher plugin;

    public MergeCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            return true;
        }
        if (!sender.hasPermission("punisher.merge")) {
            return true;
        }

        plugin.getSql().convertToNewDataStructure();
        return true;
    }
}