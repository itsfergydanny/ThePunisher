package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        Player target = plugin.getServer().getPlayer(args[0]);
        String[] argList = Arrays.copyOfRange(args, 1, args.length);
        String reason = String.join(" ", argList);

        if (target != null) {
            boolean shouldLog = true;
            target.kickPlayer(Chat.format("&cYou have been kicked for: " + reason));
            sender.sendMessage(Chat.format("&aYou have succesfully kicked " + target + " for " + reason + "!"));

            for (String str : dontLogReasons) {
                if (reason.contains(str)) {
                    shouldLog = false;
                }
            }

            if (shouldLog) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                            PreparedStatement pst = con.prepareStatement("INSERT INTO `kicks` (`id`, `ign`, `uuid`, `reason`, `time`) VALUES (NULL, '" + target.getName() + "', '" + target.getUniqueId() + "', '" + reason + "', CURRENT_TIMESTAMP)");
                            pst.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return true;
        }

        sender.sendMessage(Chat.format("&cPlayer not found."));
        return true;
    }
}
