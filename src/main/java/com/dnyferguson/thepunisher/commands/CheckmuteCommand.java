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

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");

        String muteType = plugin.getSql().getTargetType(target);

        check(target, muteType, sender);

        return false;
    }

    private void check(String target, String muteType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    boolean bypassing = false;
                    boolean temporary = false;
                    boolean muted = false;
                    String until = "";
                    String reason = "";
                    String date = "";
                    String punisher = "";

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `" + muteType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        muted = true;
                        reason = rs.getString("reason");
                        date = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";
                        punisher = rs.getString("punisher_ign");
                        if (rs.getTimestamp("until") != null) {
                            temporary = true;
                            until = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST";
                        }

                        pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `" + muteType + "` = '" + target + "' AND `active` = 1");
                        rs = pst.executeQuery();
                        if (rs.next()) {
                            bypassing = true;
                        }
                    }
                    if (muted) {
                        StringBuilder message = new StringBuilder();
                        message.append("&8&m------------------------------");
                        message.append("\n");
                        message.append("&6Player: &e");
                        message.append(target);
                        message.append("\n");
                        message.append("&6Status: &cMuted");
                        message.append("\n");
                        message.append("&6Reason: &e");
                        message.append(reason);
                        message.append("\n");
                        message.append("&6Muted by: &e");
                        message.append(punisher);
                        message.append("\n");
                        message.append("&6Date: &e");
                        message.append(date);
                        if (temporary) {
                            message.append("\n");
                            message.append("&6Until: &e");
                            message.append(until);
                        }
                        message.append("\n");
                        if (bypassing) {
                            message.append("&6Bypassing: &aUser is bypassing mute");
                        }
                        message.append("\n");
                        message.append("&8&m------------------------------");
                        sender.sendMessage(Chat.format(message.toString()));
                    } else {
                        String message = "&8&m------------------------------" +
                                "\n" +
                                "&6Player: &e" +
                                target +
                                "\n" +
                                "&6Status: &aNot Muted" +
                                "\n" +
                                "&8&m------------------------------";
                        sender.sendMessage(Chat.format(message));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
