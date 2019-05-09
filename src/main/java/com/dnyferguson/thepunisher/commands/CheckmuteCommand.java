package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class CheckmuteCommand implements CommandExecutor {

    private ThePunisher plugin;

    public CheckmuteCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.checkmute")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /checkmute (username/uuid/ip)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");

        String muteType = plugin.getSql().getTargetType(target);

        check(target, muteType, sender);

        return false;
    }

    private void check(String target, String muteType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `" + muteType + "` = '" + target + "'");
                    ResultSet rs = pst.executeQuery();
                    while(rs.next()) {
                        if (rs.getBoolean("active")) {
                            if (rs.getTimestamp("until") != null) {
                                sender.sendMessage(Chat.format("&6Player: &e" + target + "\n&6Status: &cMuted\n&6Muted by: &e" + rs.getString("punisher_ign") + "\n&6Reason: &e" + rs.getString("reason") + "\n&6Date: &e" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm:ss").format(rs.getTimestamp("time")) + "\n&6Expires: &e" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until"))));
                            } else {
                                sender.sendMessage(Chat.format("&6Player: &e" + target + "\n&6Status: &cMuted\n&6Muted by: &e" + rs.getString("punisher_ign") + "\n&6Reason: &e" + rs.getString("reason") + "\n&6Date: &e" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm:ss").format(rs.getTimestamp("time")) + "\n&6Expires: &eNever (Permanent)"));
                            }
                            return;
                        }
                    }
                    sender.sendMessage(Chat.format("&6Player: &e" + target + "\n&6Status: &aNot Muted"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}