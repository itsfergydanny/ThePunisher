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

public class CheckbanCommand implements CommandExecutor {

    private ThePunisher plugin;

    public CheckbanCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.checkban")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /checkban (username/uuid/ip)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");

        String banType = plugin.getSql().getTargetType(target);

        check(target, banType, sender);

        return false;
    }

    private void check(String target, String banType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    boolean bypassing = false;
                    boolean temporary = false;
                    boolean banned = false;
                    String until = "";
                    String reason = "";
                    String date = "";
                    String punisher = "";

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `" + banType + "` = '" + target + "' AND `active` = 1 AND `type` = 'ban'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        banned = true;
                        reason = rs.getString("reason");
                        date = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";
                        punisher = rs.getString("punisher_ign");
                        if (rs.getTimestamp("until") != null) {
                            temporary = true;
                            until = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST";
                        }

                        pst = con.prepareStatement("SELECT * FROM `bypass_ban` WHERE `" + banType + "` = '" + target + "' AND `active` = 1");
                        rs = pst.executeQuery();
                        if (rs.next()) {
                            bypassing = true;
                        }
                    }
                    if (banned) {
                        StringBuilder message = new StringBuilder();
                        message.append("&8&m------------------------------");
                        message.append("\n");
                        message.append("&6Player: &e");
                        message.append(target);
                        message.append("\n");
                        message.append("&6Status: &cBanned");
                        message.append("\n");
                        message.append("&6Reason: &e");
                        message.append(reason);
                        message.append("\n");
                        message.append("&6Banned by: &e");
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
                            message.append("&6Bypassing: &aUser is bypassing ban");
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
                                "&6Status: &aNot Banned" +
                                "\n" +
                                "&8&m------------------------------";
                        sender.sendMessage(Chat.format(message));
                    }
//                    plugin.logToDiscord(sender.getName(), "Check Ban (/checkban)", "User " + sender.getName() + " has checked " + target + "'s ban status.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
