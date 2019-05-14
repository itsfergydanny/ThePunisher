package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerChat implements Listener {

    private ThePunisher plugin;

    public PlayerChat(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @EventHandler
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

                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        Timestamp now = new Timestamp(new Date().getTime());
                        Timestamp until = rs.getTimestamp("until");
                        if (until != null) {
                            if (now.getTime() > until.getTime()) {
                                pst = con.prepareStatement("UPDATE `mutes` SET `active`=0 WHERE `uuid` = '" + uuid + "'");
                                pst.execute();
                                plugin.getMutedPlayers().remove(uuid);
                                player.sendMessage(Chat.format("&aYour mute has expired and you can now speak again!"));
                                return;
                            }
                            player.sendMessage(Chat.format("&cYou can\'t speak because you have been muted for &7" + rs.getString("reason") + " &cyour mute expires on &7" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(rs.getTimestamp("until")) + " EST"));
                            return;
                        }
                        player.sendMessage(Chat.format("&cYou can't speak because you have been muted for &7" + rs.getString("reason")));
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
