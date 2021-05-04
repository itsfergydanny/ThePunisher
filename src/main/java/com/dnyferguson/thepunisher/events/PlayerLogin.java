package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.database.FindResultCallback;
import com.dnyferguson.thepunisher.interfaces.UserBannedCallback;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PlayerLogin implements Listener {

    private ThePunisher plugin;
    private List<String> excludedIps;
    private String loginMessagePermBan;
    private String loginMessageTempBan;

    public PlayerLogin(ThePunisher plugin) {
        this.plugin = plugin;
        excludedIps = plugin.getConfig().getStringList("excluded-ips");
        loginMessagePermBan = plugin.getConfig().getString("messages.punisher-perm-ban");
        loginMessageTempBan = plugin.getConfig().getString("messages.punisher-temp-ban");
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        String username = e.getName();
        UUID uuid = e.getUniqueId();
        String ip = e.getAddress().getHostAddress();

        checkMute(uuid, ip);

        checkBan(username, uuid.toString(), ip, new UserBannedCallback() {
            @Override
            public void denyLogin(String punisher, String reason, String punishedIgn) {
                String message = Chat.format(loginMessagePermBan).replace("%reason%", reason).replace("%punishedIgn%", punishedIgn).replace("%punisher%", punisher);
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }

            @Override
            public void denyLogin(String punisher, String reason, Timestamp until, String punishedIgn) {
                String message = Chat.format(loginMessageTempBan).replace("%reason%", reason).replace("%punishedIgn%", punishedIgn).replace("%punisher%", punisher).replace("%until%", new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(until));
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }
        });
    }

    private void checkMute(UUID uuid, String ip) {
        final boolean[] bypassing = {false};
        if (excludedIps.contains(ip)) {
            bypassing[0] = true;
        }

        plugin.getSql().getResultAsync("SELECT * FROM `bypass_mute` WHERE `uuid`='" + uuid + "' AND `active`='1'", new FindResultCallback() {
            @Override
            public void onQueryDone(ResultSet result) throws SQLException {
                if (result.next()) {
                    bypassing[0] = true;
                }
            }
        });

        if (bypassing[0]) {
            return;
        }

        final boolean[] muted = {false};

        plugin.getSql().getResultAsync("SELECT * FROM `punishments` WHERE `ip`='" + ip + "' AND `type`='mute' AND `active`='1'", new FindResultCallback() {
            @Override
            public void onQueryDone(ResultSet result) throws SQLException {
                if (result.next()) {
                    plugin.getMutedPlayers().add(uuid.toString());
                    muted[0] = true;
                }
            }
        });

        if (muted[0]) {
            return;
        }

        plugin.getSql().getResultAsync("SELECT * FROM `punishments` WHERE `uuid` = '" + uuid + "' AND `active` = 1 AND `type` = 'mute'", new FindResultCallback() {
            @Override
            public void onQueryDone(ResultSet result) throws SQLException {
                if (result.next()) {
                    plugin.getMutedPlayers().add(uuid.toString());
                }
            }
        });
    }

    private void checkBan(String username, String uuid, String ip, UserBannedCallback callback) {
        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
            boolean bypassBan = false;

            // Let in if in bypassban
            PreparedStatement pst = con.prepareStatement("SELECT * FROM `bypass_ban` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                bypassBan = true;
            }

            // Check if excluded ip
            if (excludedIps.contains(ip)) {
                bypassBan = true;
            }

            pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `ip` = '" + ip + "' AND `active` = 1 AND `type` = 'ban'");
            rs = pst.executeQuery();
            if (rs.next()) {
                if (!bypassBan) {
                    Timestamp now = new Timestamp(new Date().getTime());
                    Timestamp until = rs.getTimestamp("until");
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `ip` = '" + ip + "' AND `type` = 'ban'");
                            pst.execute();
                        } else {
                            callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getTimestamp("until"), rs.getString("ign"));
                        }
                    } else {
                        callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getString("ign"));
                    }
                }
            }

            // Check if uuid is banned
            pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `uuid` = '" + uuid + "' AND `active` = 1 AND `type` = 'ban'");
            rs = pst.executeQuery();
            if (rs.next()) {
                if (!bypassBan) {
                    Timestamp now = new Timestamp(new Date().getTime());
                    Timestamp until = rs.getTimestamp("until");
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "' AND `type` = 'ban'");
                            pst.execute();
                        } else {
                            callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getTimestamp("until"), rs.getString("ign"));
                        }
                    } else {
                        callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getString("ign"));
                    }
                }
            }

            // update user in users table
            plugin.getSql().getResultAsync("SELECT * FROM `users` WHERE `uuid` = '" + uuid + "'", new FindResultCallback() {
                @Override
                public void onQueryDone(ResultSet result) throws SQLException {
                    if (result.next()) {
                        plugin.getSql().executeStatementAsync("UPDATE `users` SET `ign`='" + username + "',`ip`='" + ip + "' WHERE `uuid` = '" + uuid + "'");
                        return;
                    }
                    plugin.getSql().executeStatementAsync("INSERT INTO `users` (`id`, `ign`, `uuid`, `ip`) VALUES (NULL, '" + username + "', '" + uuid + "', '" + ip + "')");
                }
            });

            // Add login to history
            plugin.getSql().executeStatementAsync("INSERT INTO `logins` (`id`, `ign`, `uuid`, `ip`, `time`) VALUES (NULL, '" + username + "', '" + uuid + "', '" + ip + "', CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
