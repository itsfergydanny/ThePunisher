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

public class BypassMuteCommand implements CommandExecutor {

    private ThePunisher plugin;

    public BypassMuteCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.bypassmute")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /bypassmute (username/uuid)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.\\-_]", "");
        String targetType = plugin.getSql().getTargetType(target);

        String punisherIgn = "Console";
        String punisherUuid = "";

        if (sender instanceof Player) {
            Player player = (Player) sender;
            punisherIgn = player.getName();
            punisherUuid = player.getUniqueId().toString();
        }

        addBypass(sender, target, targetType, punisherIgn, punisherUuid);
        return true;
    }

    private void addBypass(CommandSender sender, String target, String targetType, String punisherIgn, String punisherUuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    // Check if already bypassing mute
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        sender.sendMessage(Chat.format("&cPlayer is already bypassing mutes."));
                        return;
                    }

                    String name = "Console";
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        name = player.getName();
                    }

                    pst = con.prepareStatement("SELECT * FROM `users` WHERE `" + targetType + "` = '" + target + "'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        String ign = rs.getString("ign");
                        String uuid = rs.getString("uuid");
                        pst = con.prepareStatement("INSERT INTO `bypass_mute` (`id`, `ign`, `uuid`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + punisherIgn + "', '" + punisherUuid + "', '1', " + "CURRENT_TIMESTAMP, '', '', NULL)");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully added " + target + " to the bypass mutes list."));
                        plugin.getRedis().sendMessage("alertstaff/" + "&c[Staff] &7" + name + "&c has granted &7" + ign + "&c the ability to bypass mutes!");
                        plugin.logToDiscord(sender.getName(), "Grant Bypass Mute (/bypassmute)", sender.getName() + " has granted the user " + target + " a mute bypass.");
                    } else {
                        sender.sendMessage(Chat.format("&cPlayer not found."));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
