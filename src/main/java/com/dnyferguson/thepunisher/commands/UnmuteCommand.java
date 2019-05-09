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

public class UnmuteCommand implements CommandExecutor {

    private ThePunisher plugin;

    public UnmuteCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.unmute")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /unmute (username/uuid/ip)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");

        String muteType = plugin.getSql().getTargetType(target);

        unmute(target, muteType, sender);

        return true;
    }

    private void unmute(String target, String muteType, CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try(Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String removerIgn = "Console";
                    String removerUuid = "";

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        removerIgn = player.getName();
                        removerUuid = player.getUniqueId().toString();
                    }

                    boolean found = false;

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `" + muteType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");

                        found = true;
                        pst = con.prepareStatement("UPDATE `mutes` SET `active`='0',`remover_ign`='" + removerIgn + "',`remover_uuid`='" + removerUuid + "',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "'");
                        pst.execute();

                        plugin.getMutedPlayers().remove(uuid);
                    }

                    if (found) {
                        sender.sendMessage(Chat.format("&aSuccessfully unmuted " + target + "!"));
                    } else {
                        sender.sendMessage(Chat.format("&cPlayer not found or not muted."));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}