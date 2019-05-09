package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.interfaces.UserBannedCallback;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.*;

public class PlayerLogin implements Listener {

    private ThePunisher plugin;

    public PlayerLogin(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        String username = e.getName();
        String uuid = e.getUniqueId().toString();
        String ip = e.getAddress().getHostAddress();

        checkUser(username, uuid, ip, new UserBannedCallback() {
            @Override
            public void denyLogin(String punisher, String reason) {
                String message = "perm ban";
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }

            @Override
            public void denyLogin(String punisher, String reason, Timestamp until) {
                String message = "temp ban";
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }
        });
    }

    private void checkUser(String username, String uuid, String ip, UserBannedCallback callback) {
        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
            // Check if ip is banned first
            PreparedStatement pst = con.prepareStatement("SELECT * FROM `bans` WHERE `ip` = '" + ip + "'");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("active")) {
                    if (rs.getTimestamp("until") != null) {
                        callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getTimestamp("time"));
                    }
                    callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"));
                    return;
                }
            }
            // Then check if uuid is present and if its banned or not
            pst = con.prepareStatement("SELECT * FROM `bans` WHERE `uuid` = '" + uuid + "'");
            rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("active")) {
                    if (rs.getTimestamp("until") != null) {
                        callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getTimestamp("time"));
                    }
                    callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"));
                    return;
                }
            }

            // Check if ip is muted first
            pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `ip` = '" + ip + "'");
            rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("active")) {
                    plugin.getMutedPlayers().add(uuid);
                    return;
                }
            }
            // Then check if uuid is present and if its muted or not
            pst = con.prepareStatement("SELECT * FROM `mutes` WHERE `uuid` = '" + uuid + "'");
            rs = pst.executeQuery();
            while (rs.next()) {
                if (rs.getBoolean("active")) {
                    plugin.getMutedPlayers().add(uuid);
                    return;
                }
            }

            // Check if user exists, if so update ign and ip. If not, create.
            pst = con.prepareStatement("SELECT * FROM `users` WHERE `uuid` = '" + uuid + "'");
            rs = pst.executeQuery();
            if (rs.next()) {
                pst = con.prepareStatement("UPDATE `users` SET `ign`='" + username + "',`ip`='" + ip + "' WHERE `uuid` = '" + uuid + "'");
                pst.execute();
            } else {
                pst = con.prepareStatement("INSERT INTO `users` (`id`, `ign`, `uuid`, `ip`) VALUES (NULL, '" + username + "', '" + uuid + "', '" + ip + "')");
                pst.execute();
            }
            // Add login to history
            pst = con.prepareStatement("INSERT INTO `logins` (`id`, `ign`, `uuid`, `ip`, `time`) VALUES (NULL, '" + username + "', '" + uuid + "', '" + ip + "', CURRENT_TIMESTAMP)");
            pst.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
