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
import java.util.ArrayList;
import java.util.List;

public class AltsCommand implements CommandExecutor {

    private ThePunisher plugin;
    private List<String> excludedIps = new ArrayList<>();

    public AltsCommand(ThePunisher plugin) {
        this.plugin = plugin;
        excludedIps = plugin.getConfig().getStringList("excluded-ips");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.alts")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /alts (username/uuid/ip)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");
        String targetType = plugin.getSql().getTargetType(target);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        StringBuilder alts = new StringBuilder();
                        String delimiter = "&e";

                        String ip = rs.getString("ip");
                        if (ip.isEmpty() || excludedIps.contains(ip)) {
                            sender.sendMessage(Chat.format("&cAlts not found."));
                            return;
                        }

                        pst = con.prepareStatement("SELECT * FROM `users` WHERE `ip` = '" + ip + "'");
                        rs = pst.executeQuery();
                        while (rs.next()) {
                            String ign = rs.getString("ign");

                            alts.append(delimiter);
                            delimiter = "&7, &e";

                            if (plugin.getServer().getPlayer(ign) != null) {
                                alts.append("&a");
                            }

                            alts.append(ign);
                        }

                        sender.sendMessage(Chat.format("&6Alts (Green = online): " + alts.toString()));
                        plugin.logToDiscord(sender.getName(), "Alt Check (/alts)", "User " + sender.getName() + " has performed an alt check on " + target + ".");
                        return;
                    }
                    sender.sendMessage(Chat.format("&cPlayer not found."));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }
}
