package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerChat implements Listener {

    private ThePunisher plugin;
    private String messagePermMute;
    private String messageTempMute;

    public PlayerChat(ThePunisher plugin) {
        this.plugin = plugin;
        messagePermMute = plugin.getConfig().getString("messages.punisher-perm-mute");
        messageTempMute = plugin.getConfig().getString("messages.punisher-temp-mute");
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (plugin.getMutedPlayers().contains(player.getUniqueId().toString())) {
            e.setCancelled(true);
            checkIfStillMuted(player);
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
                        player.sendMessage(Chat.format("&aYou are bypassing a mute. Your next messages will send!"));
                        return;
                    }

                    pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `ip` = '" + player.getAddress().getAddress().getHostAddress() + "' AND `active` = 1 AND `type` = 'mute'");
                    rs = pst.executeQuery();
                    if (rs.next()) {
                        Timestamp now = new Timestamp(new Date().getTime());
                        Timestamp until = rs.getTimestamp("until");
                        if (until != null) {
                            if (now.getTime() > until.getTime()) {
                                pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "' AND `type` = 'mute'");
                                pst.execute();
                                plugin.getMutedPlayers().remove(uuid);
                                player.sendMessage(Chat.format("&aYour mute has expired and you can now speak again!"));
                                return;
                            }
                            player.sendMessage(Chat.format(messageTempMute.replace("%reason%", rs.getString("reason")).replace("%until%", new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")))));
                            return;
                        }
                        player.sendMessage(Chat.format(messagePermMute.replace("%reason%", rs.getString("reason"))));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
