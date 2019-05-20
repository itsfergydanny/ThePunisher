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

public class UnWarnCommand implements CommandExecutor {

    private ThePunisher plugin;

    public UnWarnCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.unwarn")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /unwarn (username/uuid).\nExample: /unwarn meanie"));
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

        unwarn(sender, target, targetType, punisherIgn, punisherUuid);
        return true;
    }

    private void unwarn(CommandSender sender, String target, String targetType, String punisherIgn, String punisherUuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `" + targetType + "` = '" + target + "' AND `type` = 'warn' ORDER BY `time` DESC LIMIT 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        String reason = rs.getString("reason");
                        String punishedIgn = rs.getString("ign");
                        String punishedUuid = rs.getString("uuid");

                        pst = con.prepareStatement("INSERT INTO `admin_logs` (`id`, `punisher_ign`, `punisher_uuid`, `action`, `time`) VALUES (NULL, '" + punisherIgn + "', '" + punisherUuid + "', '" +
                                "Removed warning" + " (" + id + ")" +  " User: " + punishedIgn + ", UUID: " + punishedUuid + ", Reason: " + reason + ")', CURRENT_TIMESTAMP)");
                        pst.execute();

                        pst = con.prepareStatement("DELETE FROM `punishments` WHERE `id` = '" + id + "' AND `type` = 'warn'");
                        pst.execute();
                        sender.sendMessage(Chat.format("&aSuccessfully deleted " + target + "\'s latest warning."));
                        plugin.getRedis().sendMessage("alertstaff/" + "&c[Staff] &7" + punisherIgn + "&c has unwarned &7" + punishedIgn + "&c!");
                    } else {
                        sender.sendMessage(Chat.format("&cPlayer did not have any warnings."));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
