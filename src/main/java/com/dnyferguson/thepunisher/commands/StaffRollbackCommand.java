package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Date;

public class StaffRollbackCommand implements CommandExecutor {

    private ThePunisher plugin;

    public StaffRollbackCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.staffrollback")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /staffrollback (username/uuid) (bans/warns/mutes/all) (time).\nExample: /staffrollback meanie bans 7d"));
            return true;
        }

        String punishmentType = "";
        switch (args[1].toLowerCase()) {
            case "bans":
                punishmentType = "bans";
                break;
            case "warns":
                punishmentType = "warns";
                break;
            case "mutes":
                punishmentType = "mutes";
                break;
            case "all":
                punishmentType = "all";
                break;
        }

        if (punishmentType.isEmpty()) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /staffrollback (username/uuid) (bans/warns/mutes/all) (time).\nExample: /staffrollback meanie bans 7d"));
            return true;
        }

        Timestamp time = new Timestamp(new Date().getTime());

        try {
            if (args[2].endsWith("d") && Integer.parseInt(args[2].split("d")[0]) != 0) {
                time = com.dnyferguson.thepunisher.utils.Time.getBackwards(args[2]);
            }
            if (args[2].endsWith("h") && Integer.parseInt(args[2].split("h")[0]) != 0) {
                time = com.dnyferguson.thepunisher.utils.Time.getBackwards(args[2]);
            }
            if (args[2].endsWith("m") && Integer.parseInt(args[2].split("m")[0]) != 0) {
                time = com.dnyferguson.thepunisher.utils.Time.getBackwards(args[2]);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /staffrollback (username/uuid) (bans/warns/mutes/all) (time).\nExample: /staffrollback meanie bans 7d"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");
        String targetType = plugin.getSql().getTargetType(target);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        rollback(sender, target, targetType, punishmentType, punisherIgn, punisherUuid, time);
        return true;
    }

    private void rollback(CommandSender sender, String target, String targetType, String punishmentType, String punisherIgn, String punisherUuid, Timestamp time) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    // First we get target profile
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    int count = 0;

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                        ip = rs.getString("ip");
                    }

                    // Then we handle rollback "all"
                    if (punishmentType.equals("all")) {
                        count += rollbackType("bans", uuid, time, punisherIgn, punisherUuid);
                        count += rollbackType("mutes", uuid, time, punisherIgn, punisherUuid);
                        count += rollbackType("warns", uuid, time, punisherIgn, punisherUuid);
                        count += rollbackType("kicks", uuid, time, punisherIgn, punisherUuid);
                        sender.sendMessage(Chat.format("&aRolled back " + count + " punishments from " + ign + " (" + uuid + ")."));
                        return;
                    }
                    // Finally rollback based on specific type
                    count = rollbackType(punishmentType, uuid, time, punisherIgn, punisherUuid);
                    sender.sendMessage(Chat.format("&aRolled back " + count + " punishments from " + ign + " (" + uuid + ")."));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private int rollbackType(String punishmentType, String uuid, Timestamp time, String punisherIgn, String punisherUuid) {
        try (Connection con = plugin.getSql().getDatasource().getConnection()){
            int count = 0;

            PreparedStatement pst = con.prepareStatement("SELECT * FROM `" + punishmentType + "` WHERE `punisher_uuid` = '" + uuid + "'");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Timestamp punishmentTime = rs.getTimestamp("time");
                int id = rs.getInt("id");
                String reason = rs.getString("reason");
                String punishedIgn = rs.getString("ign");
                String punishedUuid = rs.getString("uuid");

                if (punishmentTime.getTime() > time.getTime()) {
                    count++;

                    pst = con.prepareStatement("INSERT INTO `admin_logs` (`id`, `punisher_ign`, `punisher_uuid`, `action`, `time`) VALUES (NULL, '" + punisherIgn + "', '" + punisherUuid + "', '" +
                            "Rolled back " + punishmentType + " (" + id + ")" +  " User: " + punishedIgn + ", UUID: " + punishedUuid + ", Reason: " + reason + ")', CURRENT_TIMESTAMP)");
                    pst.execute();

                    pst = con.prepareStatement("DELETE FROM `" + punishmentType + "` WHERE `id` = '" + id + "'");
                    pst.execute();
                }
            }

            return count;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}