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

public class UnbypassMuteCommand implements CommandExecutor {

    private ThePunisher plugin;

    public UnbypassMuteCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("punisher.unbypassmute")) {
            sender.sendMessage(Chat.format("&cYou don\'t have permission to do this."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Chat.format("&cInvalid syntax. Use /unbypassmute (username/uuid)"));
            return true;
        }

        String target = args[0].replaceAll("[^0-9a-zA-Z\\.-]", "");

        String targetType = plugin.getSql().getTargetType(target);

        unbypass(target, targetType, sender);

        return true;
    }

    private void unbypass(String target, String targetType, CommandSender sender) {
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

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        found = true;
                        pst = con.prepareStatement("UPDATE `bypass_mute` SET `active`='0',`remover_ign`='" + removerIgn + "',`remover_uuid`='" + removerUuid + "',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + rs.getString("uuid") + "'");
                        pst.execute();
                    }

                    if (found) {
                        sender.sendMessage(Chat.format("&aSuccessfully removed " + target + " from the bypass mutes list!"));
                    } else {
                        sender.sendMessage(Chat.format("&cPlayer not found or not bypassing bans."));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}