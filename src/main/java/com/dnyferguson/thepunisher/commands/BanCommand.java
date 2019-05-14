package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class BanCommand implements CommandExecutor {

    private ThePunisher plugin;

    public BanCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.ban")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /ban (username/uuid/ip) (time [optional]) (reason).\nExample: /ban meanie 7d (reason)"));
            return true;
        }

        boolean isTemporaryPunishment = false;
        Timestamp until = new Timestamp(new Date().getTime());

        try {
            if (args[1].endsWith("d") && Integer.parseInt(args[1].split("d")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("h") && Integer.parseInt(args[1].split("h")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("m") && Integer.parseInt(args[1].split("m")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
        } catch (NumberFormatException e) {}


        String[] argList;
        if (isTemporaryPunishment) {
            if (args.length < 3) {
                sender.sendMessage(Chat.format("&cInvalid syntax. Use /ban (username/uuid/ip) (time [optional]) (reason).\nExample: /ban meanie 7d (reason)"));
                return true;
            }
            argList = Arrays.copyOfRange(args, 2, args.length);
        } else {
            argList = Arrays.copyOfRange(args, 1, args.length);
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");
        String targetType = plugin.getSql().getTargetType(target);
        String reason = String.join(" ", argList);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        addBan(sender, target, targetType, reason, punisherIgn, punisherUuid, isTemporaryPunishment, until);

        return true;
    }

    private void addBan(CommandSender sender, String target, String targetType, String reason, String punisherIgn, String punisherUUID, boolean isTemporaryPunishment, Timestamp until) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    // Check if already banned
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bans` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        sender.sendMessage(Chat.format("&cPlayer is already banned."));
                        return;
                    }

                    pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                        ip = rs.getString("ip");
                    }

                    if (isTemporaryPunishment) {
                        pst = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                                " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL," +
                                " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, '" + until + "', '" + ip + "', '', '', NULL)");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully banned " + target + " for " + reason + " until " + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(until) + " EST!"));
                    } else {
                        pst = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                                " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL," +
                                " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, NULL, '" + ip + "', '', '', NULL)");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully banned " + target + " for " + reason + "!"));
                    }
                    plugin.getRedis().sendMessage("ban/" + uuid + "/" + reason);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
