package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class HistoryCommand implements CommandExecutor {

    private ThePunisher plugin;

    public HistoryCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.history")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /history (username/uuid/ip) (limit [optional]).\nExample: /history meanie"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");
        String targetType = plugin.getSql().getTargetType(target);

        String limit = "";
        if (args.length > 1) {
            limit = args[1];
        }

        if (limit.isEmpty()) {
            checkHistory(target, targetType, sender, "5");
        } else {
            checkHistory(target, targetType, sender, limit);
        }

        return true;
    }

    private void checkHistory(String target, String targetType, CommandSender sender, String limit) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {

                    String type = targetType;
                    String player = target;


                    StringBuilder message = new StringBuilder();
                    int count = 0;

                    PreparedStatement pst = con.prepareStatement("SELECT * from `users` where `" + targetType + "` = '" + target + "'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                       type = "uuid";
                       player = rs.getString("uuid");
                    }

                    // Get bans
                    message.append("&c&lBans:\n");
                    pst = con.prepareStatement("SELECT * from `bans` where `" + type + "` = '" + player + "' ORDER BY `time` DESC LIMIT " + limit);
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        count++;
                        String reason = rs.getString("reason");
                        String punisherIgn = rs.getString("punisher_ign");
                        boolean active = rs.getBoolean("active");
                        String time = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";

                        message.append("&cBanned on ");
                        message.append(time);
                        message.append(" by &7");
                        message.append(punisherIgn);
                        message.append("&c for &7");
                        message.append(reason);
                        message.append("&c.\n");

                        if (!active) {
                            if (rs.getString("remover_ign").equals("#expired") || rs.getString("remover_ign").isEmpty()) {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST";
                                message.append("&aExpired on ");
                                message.append(removedTime);
                                message.append("&a.\n");
                            } else {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("removed_time")) + " EST";
                                String removerIgn = rs.getString("remover_ign");
                                message.append("&aUnbanned on ");
                                message.append(removedTime);
                                message.append(" by &7");
                                message.append(removerIgn);
                                message.append("&a.\n");
                            }
                        }
                    }

                    if (count < 1) {
                        message.append("&7(None)");
                    }

                    // Get mutes
                    message.append("\n \n&c&lMutes:\n");
                    count = 0;
                    pst = con.prepareStatement("SELECT * from `mutes` where `" + type + "` = '" + player + "' ORDER BY `time` DESC LIMIT " + limit);
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        count++;
                        String reason = rs.getString("reason");
                        String punisherIgn = rs.getString("punisher_ign");
                        boolean active = rs.getBoolean("active");
                        String time = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";

                        message.append("&cMuted on ");
                        message.append(time);
                        message.append(" by &7");
                        message.append(punisherIgn);
                        message.append("&c for &7");
                        message.append(reason);
                        message.append("&c.\n");

                        if (!active) {
                            if (rs.getString("remover_ign").equals("#expired") || rs.getString("remover_ign").isEmpty()) {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST";
                                message.append("&aExpired on ");
                                message.append(removedTime);
                                message.append("&a.\n");
                            } else {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("removed_time")) + " EST";
                                String removerIgn = rs.getString("remover_ign");
                                message.append("&aUnmuted on ");
                                message.append(removedTime);
                                message.append(" by &7");
                                message.append(removerIgn);
                                message.append("&a.\n");
                            }
                        }
                    }

                    if (count < 1) {
                        message.append("&7(None)");
                    }

                    // Get warns
                    message.append("\n \n&c&lWarns:\n");
                    count = 0;
                    pst = con.prepareStatement("SELECT * from `warns` where `" + type + "` = '" + player + "' ORDER BY `time` DESC LIMIT " + limit);
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        count++;
                        String reason = rs.getString("reason");
                        String punisherIgn = rs.getString("punisher_ign");
                        boolean active = rs.getBoolean("active");
                        String time = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";

                        message.append("&cWarned on ");
                        message.append(time);
                        message.append(" by &7");
                        message.append(punisherIgn);
                        message.append("&c for &7");
                        message.append(reason);
                        message.append("&c.\n");

                        if (!active) {
                            if (rs.getString("remover_ign").equals("#expired") || rs.getString("remover_ign").isEmpty()) {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST";
                                message.append("&aExpired on ");
                                message.append(removedTime);
                                message.append("&a.\n");
                            } else {
                                String removedTime = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("removed_time")) + " EST";
                                String removerIgn = rs.getString("remover_ign");
                                message.append("&aUnwarned on ");
                                message.append(removedTime);
                                message.append(" by &7");
                                message.append(removerIgn);
                                message.append("&a.\n");
                            }
                        }
                    }

                    if (count < 1) {
                        message.append("&7(None)");
                    }

                    // Get kicks
                    message.append("\n \n&c&lKicks:\n");
                    count = 0;
                    pst = con.prepareStatement("SELECT * from `kicks` where `" + type + "` = '" + player + "' ORDER BY `time` DESC LIMIT " + limit);
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        count++;
                        String reason = rs.getString("reason");
                        String punisherIgn = rs.getString("punisher_ign");
                        String time = new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("time")) + " EST";

                        message.append("&cKicked on ");
                        message.append(time);
                        message.append(" by &7");
                        message.append(punisherIgn);
                        message.append("&c for &7");
                        message.append(reason);
                        message.append("&c.\n");
                    }

                    if (count < 1) {
                        message.append("&7(None)");
                    }

                    sender.sendMessage(Chat.format(message.toString()));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
