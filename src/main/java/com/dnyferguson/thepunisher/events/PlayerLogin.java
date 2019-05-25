package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.interfaces.UserBannedCallback;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            public void denyLogin(String punisher, String reason, String punishedIgn) {
                String message = Chat.format("\n&cYou have been permanently banned for: \n&7" + reason + "&c.\n" +
                        "Under the name: &7" + punishedIgn + "&c.\nBy staff member: &7" + punisher + "&c.\n \n&aAppeal at bit.ly/mmt-appeal or\n" +
                        " by emailing support@momentonetwork.net or\n Purchase an instant unban at bit.ly/mmt-buyunban");
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }

            @Override
            public void denyLogin(String punisher, String reason, Timestamp until, String punishedIgn) {
                String message = Chat.format("\n&cYou have been temporarily banned for: \n&7" + reason + "&c.\n" +
                        "Under the name: &7" + punishedIgn + "&c.\nBy staff member: &7" + punisher + "&c.\n \n&eExpires: &7" + new SimpleDateFormat("MM/dd/yyyy @ HH:mm").format(until) + " EST&e.\n \n&aAppeal at bit.ly/mmt-appeal or\n" +
                        " by emailing support@momentonetwork.net or\n Purchase an instant unban at bit.ly/mmt-buyunban");
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            }
        });
    }

    private void checkUser(String username, String uuid, String ip, UserBannedCallback callback) {
        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
            boolean bypassBan = false;
            boolean bypassMute = false;

            System.out.println("BITCH");

            // Let in if in bypassban
            PreparedStatement pst = con.prepareStatement("SELECT * FROM `bypass_ban` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                bypassBan = true;
            }

            pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `ip` = '" + ip + "' AND `active` = 1 AND `type` = 'ban'");
            rs = pst.executeQuery();
            if (rs.next()) {
                if (!bypassBan) {
                    Timestamp now = new Timestamp(new Date().getTime());
                    Timestamp until = rs.getTimestamp("until");
                    System.out.println("UNTIL IS " + until);
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `ip` = '" + ip + "' AND `type` = 'ban'");
                            pst.execute();
                        } else {
                            System.out.println("TEMP BAN");
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
                    System.out.println("UNTIL IS " + until);
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "' AND `type` = 'ban'");
                            pst.execute();
                        } else {
                            System.out.println("TEMP BAN");
                            callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getTimestamp("until"), rs.getString("ign"));
                        }
                    } else {
                        callback.denyLogin(rs.getString("punisher_ign"), rs.getString("reason"), rs.getString("ign"));
                    }
                }
            }

            // let thru if bypassmute
            pst = con.prepareStatement("SELECT * FROM `bypass_mute` WHERE `uuid` = '" + uuid + "' AND `active` = 1");
            rs = pst.executeQuery();
            if (rs.next()) {
                bypassMute = true;
            }

            // Check if ip is muted
            pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `ip` = '" + ip + "' AND `active` = 1 AND `type` = 'mute'");
            rs = pst.executeQuery();
            if (rs.next()) {
                if (!bypassMute) {
                    Timestamp now = new Timestamp(new Date().getTime());
                    Timestamp until = rs.getTimestamp("until");
                    boolean muteStillValid = true;
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `ip` = '" + ip + "' AND `type` = 'mute'");
                            pst.execute();
                            muteStillValid = false;
                        }
                    }
                    if (muteStillValid) {
                        plugin.getMutedPlayers().add(uuid);
                    } else {
                        plugin.getMutedPlayers().remove(uuid);
                    }
                }
            } else {
                plugin.getMutedPlayers().remove(uuid);
            }

            // Check if uuid is muted
            pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `uuid` = '" + uuid + "' AND `active` = 1 AND `type` = 'mute'");
            rs = pst.executeQuery();
            if (rs.next()) {
                if (!bypassMute) {
                    Timestamp now = new Timestamp(new Date().getTime());
                    Timestamp until = rs.getTimestamp("until");
                    boolean muteStillValid = true;
                    if (until != null) {
                        if (now.getTime() > until.getTime()) {
                            pst = con.prepareStatement("UPDATE `punishments` SET `active`=0,`remover_ign`='#expired',`removed_time`=CURRENT_TIMESTAMP WHERE `uuid` = '" + uuid + "' AND `type` = 'mute'");
                            pst.execute();
                            muteStillValid = false;
                        }
                    }
                    if (muteStillValid) {
                        plugin.getMutedPlayers().add(uuid);
                    } else {
                        plugin.getMutedPlayers().remove(uuid);
                    }
                }
            } else {
                plugin.getMutedPlayers().remove(uuid);
            }

            // Update ign & last ip
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
