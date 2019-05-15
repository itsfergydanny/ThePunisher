package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlayerSendCommand implements Listener {

    private ThePunisher plugin;
    private List<String> blockedCommandsWhileMuted = new ArrayList<>();

    public PlayerSendCommand(ThePunisher plugin) {
        this.plugin = plugin;
        this.blockedCommandsWhileMuted = plugin.getConfig().getStringList("blocked-commands-when-muted");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String uuid = player.getUniqueId().toString();
        String command = e.getMessage().split(" ")[0];
        if (plugin.getMutedPlayers().contains(uuid)) {
            if (blockedCommandsWhileMuted.contains(command)) {
                e.setCancelled(true);
                checkIfStillMuted(player);
            }
        }
    }

    private void checkIfStillMuted(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                    String uuid = player.getUniqueId().toString();

                    // Let chat if in bypassmute
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        plugin.getMutedPlayers().remove(uuid);
                        player.sendMessage(Chat.format("&aYou are bypassing a mute. Your next commands will send!"));
                        return;
                    }

                    pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `ip` = '" + player.getAddress().getAddress().getHostAddress() + "' AND `active` = 1");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        Timestamp now = new Timestamp(new Date().getTime());
                        Timestamp until = rs.getTimestamp("until");
                        if (until != null) {
                            if (now.getTime() > until.getTime()) {
                                pst = con.prepareStatement("UPDATE `mutes` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "'");
                                pst.execute();
                                plugin.getMutedPlayers().remove(uuid);
                                player.sendMessage(Chat.format("&aYour mute has expired and you can now speak again!"));
                                return;
                            }
                            player.sendMessage(Chat.format("&cYou can\'t use that command because you have been muted for &7" + rs.getString("reason") + " &cyour mute expires on &7" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST"));
                            return;
                        }
                        player.sendMessage(Chat.format("&cYou can't use that command because you have been muted for &7" + rs.getString("reason")));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
