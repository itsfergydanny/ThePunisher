package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WarnCommand implements CommandExecutor {

    private ThePunisher plugin;
    private Map<Integer, String> triggerCommands = new HashMap<>();
    private int highestTrigger = 0;

    public WarnCommand(ThePunisher plugin) {
        this.plugin = plugin;
        for (String line : plugin.getConfig().getStringList("warning-triggers")) {
            String[] split = line.split(":");
            int warnCount = Integer.valueOf(split[0]);
            if (warnCount > highestTrigger) {
                highestTrigger = warnCount;
            }
            String command = split[1];
            triggerCommands.put(warnCount, command);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.warn")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /warn (username/uuid) (reason).\nExample: /warn meanie (reason)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");
        String targetType = plugin.getSql().getTargetType(target);
        String[] argList = Arrays.copyOfRange(args, 1, args.length);
        String reason = String.join(" ", argList);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        addWarn(sender, target, targetType, reason, punisherIgn, punisherUuid);
        return false;
    }

    private void addWarn(CommandSender sender, String target, String targetType, String reason, String punisherIgn, String punisherUUID) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    Timestamp now = new Timestamp(new Date().getTime());

                    // Check if already warned in last 15s
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `warns` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1 ORDER BY `time` DESC LIMIT 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        Timestamp punishmentTime = rs.getTimestamp("time");

                        if (punishmentTime.getTime() >= now.getTime() - 15000) {
                            sender.sendMessage(Chat.format("&cPlayer has already gotten a warning in the last 15s."));
                            return;
                        }
                    }

                    // Grab user details
                    pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                        ip = rs.getString("ip");
                    }

                    // Update which warnings are still valid (so fresher than 7 days)
                    pst = con.prepareStatement("SELECT * FROM `warns` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1 ORDER BY `time` DESC LIMIT 100");
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        Timestamp time = rs.getTimestamp("time");
                        int id = rs.getInt("id");
                        if (now.getTime() - (86400000 * 7) >= time.getTime()) {
//                            sender.sendMessage("Punishment of id " + id + " is expired (older than 7 days)");
//                            sender.sendMessage("Difference is : " + (now.getTime() - time.getTime()) / 86400000 + " days.");
                            pst = con.prepareStatement("UPDATE `warns` SET `active`='0' WHERE `id` = '" + id + "'");
                            pst.execute();
                        }
                    }

                    pst = con.prepareStatement("INSERT INTO `warns` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                            " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL," +
                            " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, NULL, '" + ip + "', '', '', NULL)");
                    pst.execute();
                    sender.sendMessage(Chat.format("&aSuccessfully warned " + target + " for " + reason + "!"));

                    // Punish based on warn count
                    pst = con.prepareStatement("SELECT * FROM `warns` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1 ORDER BY `time` DESC LIMIT " + highestTrigger);
                    rs = pst.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                    }

                    String punishmentCommand = "";
                    if (count >= highestTrigger) {
                        punishmentCommand = triggerCommands.get(highestTrigger).replace("/","").replace("%player%", ign).replace("%reason%", reason);
                    } else {
                        for (int triggerNumber : triggerCommands.keySet()) {
                            if (count == triggerNumber) {
                                punishmentCommand = triggerCommands.get(triggerNumber).replace("/","").replace("%player%", ign).replace("%reason%", reason);
                            }
                        }
                    }

                    dispatchCommand(punishmentCommand);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void dispatchCommand(String command) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            }
        });
    }
}
