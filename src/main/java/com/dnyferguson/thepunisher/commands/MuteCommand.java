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

public class MuteCommand implements CommandExecutor {

    private ThePunisher plugin;

    public MuteCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.mute")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /mute (username/uuid/ip) (time [optional]) (reason).\nExample: /mute meanie 7d (reason)"));
            return true;
        }

        boolean isTemporaryPunishment = false;
        Timestamp until = new Timestamp(new Date().getTime());

        try {
            if (args[1].endsWith("d") && Integer.parseInt(args[1].split("d")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("D") && Integer.parseInt(args[1].split("D")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("h") && Integer.parseInt(args[1].split("h")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("H") && Integer.parseInt(args[1].split("H")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("m") && Integer.parseInt(args[1].split("m")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
            if (args[1].endsWith("M") && Integer.parseInt(args[1].split("M")[0]) != 0) {
                isTemporaryPunishment = true;
                until = com.dnyferguson.thepunisher.utils.Time.getForwards(args[1]);
            }
        } catch (NumberFormatException e) {}


        String[] argList;
        if (isTemporaryPunishment) {
            if (args.length < 3) {
                sender.sendMessage(Chat.format("&cInvalid syntax. Use /mute (username/uuid/ip) (time [optional]) (reason).\nExample: /mute meanie 7d (reason)"));
                return true;
            }
            argList = Arrays.copyOfRange(args, 2, args.length);
        } else {
            argList = Arrays.copyOfRange(args, 1, args.length);
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");
        String targetType = plugin.getSql().getTargetType(target);
        String reason = String.join(" ", argList);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        addMute(sender, target, targetType, reason, punisherIgn, punisherUuid, isTemporaryPunishment, until);

        return true;
    }

    private void addMute(CommandSender sender, String target, String targetType, String reason, String punisherIgn, String punisherUUID, boolean isTemporaryPunishment, Timestamp until) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    // Check if already muted
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1 AND `type` = 'mute'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        sender.sendMessage(Chat.format("&cPlayer is already muted."));
                        return;
                    }

                    pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                        ip = rs.getString("ip");
                    }

                    pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        sender.sendMessage(Chat.format("&cPlayer is bypassing mutes. Please remove the bypass before punishing!"));
                        return;
                    }


                    if (isTemporaryPunishment) {
                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                                " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, 'mute'," +
                                " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, '" + until + "', '" + ip + "', '', '', NULL)");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully muted " + target + " for " + reason + " until " + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(until) + " EST!"));
                        plugin.getRedis().sendMessage("mute/" + uuid + "/" + reason + "/" + new SimpleDateFormat("MM-dd-yyyy @ HH:mm").format(until) + " EST");
                        plugin.getRedis().sendMessage("alertstaff/" + "&c[Staff] &7" + punisherIgn + "&c has muted &7" + ign + "&c for &7" + reason + "&c until &7" + new SimpleDateFormat("MM-dd-yyyy @ HH:mm").format(until) + "&c!");
                        plugin.logToDiscord(sender.getName(), "Temp Mute (/mute)", "User " + sender.getName() + " has temporarily muted " +
                                target + " for reason `" + reason + "` until " + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(until) + " EST!");
                    } else {
                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`," +
                                " `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, 'mute'," +
                                " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisherUUID + "', '1', CURRENT_TIMESTAMP, NULL, '" + ip + "', '', '', NULL)");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully muted " + target + " for " + reason + "!"));
                        plugin.getRedis().sendMessage("mute/" + uuid + "/" + reason + "/" + "");
                        plugin.getRedis().sendMessage("alertstaff/" + "&c[Staff] &7" + punisherIgn + "&c has muted &7" + ign + "&c for &7" + reason + "&c!");
                        plugin.logToDiscord(sender.getName(), "Perm Mute (/mute)", "User " + sender.getName() + " has permanently muted " +
                                target + " for reason `" + reason + "`.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
