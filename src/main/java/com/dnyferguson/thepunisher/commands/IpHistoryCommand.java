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
import java.util.HashMap;
import java.util.Map;

public class IpHistoryCommand implements CommandExecutor {

    private ThePunisher plugin;

    public IpHistoryCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.iphistory")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /iphistory (username/uuid)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");
        String targetType = plugin.getSql().getTargetType(target);

        checkHistory(target, targetType, sender);
        return true;
    }

    private void checkHistory(String target, String targetType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    Map<String, String> logins = new HashMap<>();
                    String lastIp = "";

                    PreparedStatement pst = con.prepareStatement("SELECT * from `logins` where `" + targetType + "` = '" + target + "' ORDER BY `logins`.`time` DESC LIMIT 500");
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        String ip = rs.getString("ip");
                        String date = new SimpleDateFormat("MM/dd/yyyy").format(rs.getTimestamp("time"));
                        if (!ip.equals(lastIp)) {
                            logins.put(ip, date);
                        }
                    }

                    StringBuilder message = new StringBuilder();
                    message.append("&8&m------------------------------");
                    message.append("\n");
                    message.append("&6Player: &e");
                    message.append(target);
                    message.append("\'s recent ips");

                    int count = 0;
                    for (Map.Entry<String, String> entry : logins.entrySet()) {
                        if (count < 20) {
                            count++;
                            message.append("\n &7- &a");
                            message.append(entry.getKey());
                            message.append("&2 [");
                            message.append(entry.getValue());
                            message.append("]");
                        }
                    }

                    message.append("\n");
                    message.append("&8&m------------------------------");
                    sender.sendMessage(Chat.format(message.toString()));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
