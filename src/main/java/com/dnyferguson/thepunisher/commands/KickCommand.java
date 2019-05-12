package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class KickCommand implements CommandExecutor {

    private ThePunisher plugin;
    private List<String> dontLogReasons = new ArrayList<>();

    public KickCommand(ThePunisher plugin) {
        this.plugin = plugin;
        this.dontLogReasons = plugin.getConfig().getStringList("dont-log-reasons");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.kick")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /kick (username) (reason)"));
            return true;
        }

        String target = args[0];
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

        boolean shouldLog = true;

        for (String str : dontLogReasons) {
            if (reason.contains(str)) {
                shouldLog = false;
            }
        }

        kick(target, targetType, reason, punisherIgn, punisherUuid, shouldLog, sender);
        return true;
    }

    private void kick(String target, String targetType, String reason, String punisherIgn, String punisherUuid, boolean shouldLog, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = target;
                    String ign = target;
                    String ip = target;

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                        ign = rs.getString("ign");
                    } else {
                        sender.sendMessage(Chat.format("&cPlayer not found."));
                        return;
                    }

                    plugin.getRedis().sendMessage("kick " + uuid + " " + reason);
                    sender.sendMessage(Chat.format("&aYou have succesfully kicked " + target + " for " + reason + "!"));

                    if (shouldLog) {
                        pst = con.prepareStatement("INSERT INTO `kicks` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn +  "', '" + punisherUuid + "', CURRENT_TIMESTAMP)");
                        pst.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
