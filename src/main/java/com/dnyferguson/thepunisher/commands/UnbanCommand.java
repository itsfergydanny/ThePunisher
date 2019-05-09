package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UnbanCommand implements CommandExecutor {

    private ThePunisher plugin;

    public UnbanCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.unban")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /unban (username/uuid/ip)"));
            return true;
        }
        
        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");

        String banType = "username";
        
        if (target.contains("-")) {
            banType = "uuid";
        }
        
        if (target.contains(".")) {
            banType = "ip";
        }
        
        unban(target, banType, sender);
        
        return true;
    }

    private void unban(String target, String banType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try(Connection con = plugin.getSql().getDatasource().getConnection()) {
                    switch (banType) {
                        case "uuid":
                            PreparedStatement pst = con.prepareStatement("UPDATE `bans` SET `active`='0' WHERE `uuid` = '" + target + "'");
                            pst.execute();
                            break;
                        case "ip":
                            pst = con.prepareStatement("UPDATE `bans` SET `active`='0' WHERE `ip` = '" + target + "'");
                            pst.execute();
                            break;
                        default:
                            pst = con.prepareStatement("UPDATE `bans` SET `active`='0' WHERE `ign` = '" + target + "'");
                            pst.execute();
                            break;
                    }
                    sender.sendMessage(Chat.format("&aSuccessfully unbanned " + target + "!"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
