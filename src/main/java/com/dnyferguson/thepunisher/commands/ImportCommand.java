package com.dnyferguson.thepunisher.commands;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.*;

public class ImportCommand implements CommandExecutor {

    private ThePunisher plugin;
    private HikariDataSource datasource;

    public ImportCommand(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            return true;
        }
        if (!sender.hasPermission("punisher.import")) {
            return true;
        }

        initialize();

        importBans(sender);
        return true;
    }

    private void initialize() {
        FileConfiguration cfg = plugin.getConfig();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + cfg.getString("litebans-mysql.ip") + ":" + cfg.getString("litebans-mysql.port") + "/" + cfg.getString("litebans-mysql.database"));
        config.setUsername(cfg.getString("litebans-mysql.username"));
        config.setPassword(cfg.getString("litebans-mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.datasource = new HikariDataSource(config);
    }

    private void importKicks(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection litebans = datasource.getConnection()) {
                    System.out.println("[ThePunisher] starting import of kicks from litebans!");
                    PreparedStatement pst = litebans.prepareStatement("SELECT * FROM `litebans_kicks`");
                    ResultSet rs = pst.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        String ign;
                        String uuid = rs.getString("uuid");
                        if (uuid == null || uuid.equals("#undefined#") || uuid.equals("#offline#")) {
                            uuid = "";
                        }
                        String reason = rs.getString("reason").replaceAll("'", "").replaceAll("\\\\", "");
                        String punisherIgn = rs.getString("banned_by_name");
                        String punisher_uuid = rs.getString("banned_by_uuid");
                        if (punisherIgn == null ||punisherIgn.equals("#undefined#")) {
                            punisherIgn = getLastKnownUsernameFromUUID(punisher_uuid);
                        }
                        Timestamp time = new Timestamp(rs.getLong("time"));

                        ign = getLastKnownUsernameFromUUID(uuid);

                        addToUsersTable(ign, uuid);

                        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                            PreparedStatement pst3 = con.prepareStatement("INSERT INTO `kicks` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `time`) VALUES (NULL," +
                                    " '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + time + "')");
                            pst3.execute();
                        }
                    }
                    sender.sendMessage(Chat.format("&aImported " + count + " kicks from litebans!"));
                    importWarns(sender);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void importWarns(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection litebans = datasource.getConnection()) {
                    System.out.println("[ThePunisher] starting import of warns from litebans!");
                    PreparedStatement pst = litebans.prepareStatement("SELECT * FROM `litebans_warnings`");
                    ResultSet rs = pst.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        int id = rs.getInt("id");
                        String ign;
                        String uuid = rs.getString("uuid");
                        if (uuid == null || uuid.equals("#undefined#") || uuid.equals("#offline#")) {
                            uuid = "";
                        }
                        String reason = rs.getString("reason").replaceAll("'", "").replaceAll("\\\\", "");
                        String punisherIgn = rs.getString("banned_by_name");
                        String punisher_uuid = rs.getString("banned_by_uuid");
                        if (punisherIgn == null ||punisherIgn.equals("#undefined#")) {
                            punisherIgn = getLastKnownUsernameFromUUID(punisher_uuid);
                        }
                        int active = rs.getInt("active");
                        Timestamp time = new Timestamp(rs.getLong("time"));
                        if (time.getTime() < Timestamp.valueOf("1970-01-10 00:10:00").getTime()) {
                            time = Timestamp.valueOf("1970-01-10 00:10:00");
                        }
                        Timestamp until = null;
                        Timestamp removedTime = rs.getTimestamp("removed_by_date");
                        if (rs.getLong("until") > 0) {
                            until = new Timestamp(rs.getLong("until"));
                            if (until.getTime() > Timestamp.valueOf("2038-01-10 00:00:00").getTime()) {
                                until = Timestamp.valueOf("2038-01-10 00:10:00");
                            }
                        }
                        String ip = rs.getString("ip");
                        if (ip == null || ip.equals("127.0.0.1") || ip.contains("#")) {
                            ip = "";
                        }
                        String removerUuid = rs.getString("removed_by_uuid");
                        if (removerUuid == null) {
                            removerUuid = "";
                        }
                        String removerIgn = rs.getString("removed_by_name");
                        if (removerIgn == null) {
                            removerIgn = getLastKnownUsernameFromUUID(removerUuid);
                            removedTime = null;
                        }

                        ign = getLastKnownUsernameFromUUID(uuid);

                        addToUsersTable(ign, uuid);

//                        System.out.println("[Importer] imported warning #" + id + ". Ign: " + ign + ". UUID: " + uuid + ". Reason: " + reason + ". Punisher Ign: " + punisherIgn + ". Punisher UUID: " + punisher_uuid + "" +
//                                ". Active: " + active + ". Time: " + time + ". Until: " + until + ". IP: " + ip + ". Remover Ign: " + removerIgn + ". Remover UUID: " + removerUuid + ". Remover Time: " + removedTime);

                        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                            if (until == null) {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `warns` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `warns` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            } else {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `warns` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `warns` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            }
                        }
                    }
                    sender.sendMessage(Chat.format("&aImported " + count + " warns from litebans!"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void importMutes(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection litebans = datasource.getConnection()) {
                    System.out.println("[ThePunisher] starting import of mutes from litebans!");
                    PreparedStatement pst = litebans.prepareStatement("SELECT * FROM `litebans_mutes`");
                    ResultSet rs = pst.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        int id = rs.getInt("id");
                        String ign;
                        String uuid = rs.getString("uuid");
                        if (uuid == null || uuid.equals("#undefined#") || uuid.equals("#offline#")) {
                            uuid = "";
                        }
                        String reason = rs.getString("reason").replaceAll("'", "").replaceAll("\\\\", "");
                        String punisherIgn = rs.getString("banned_by_name");
                        String punisher_uuid = rs.getString("banned_by_uuid");
                        if (punisherIgn == null || punisherIgn.equals("#undefined#")) {
                            punisherIgn = getLastKnownUsernameFromUUID(punisher_uuid);
                        }
                        int active = rs.getInt("active");
                        Timestamp time = new Timestamp(rs.getLong("time"));
                        if (time.getTime() < Timestamp.valueOf("1970-01-10 00:10:00").getTime()) {
                            time = Timestamp.valueOf("1970-01-10 00:10:00");
                        }
                        Timestamp until = null;
                        Timestamp removedTime = rs.getTimestamp("removed_by_date");
                        if (rs.getLong("until") > 0) {
                            until = new Timestamp(rs.getLong("until"));
                            if (until.getTime() > Timestamp.valueOf("2038-01-10 00:00:00").getTime()) {
                                until = Timestamp.valueOf("2038-01-10 00:10:00");
                            }
                        }
                        String ip = rs.getString("ip");
                        if (ip == null || ip.equals("127.0.0.1") || ip.contains("#")) {
                            ip = "";
                        }
                        String removerUuid = rs.getString("removed_by_uuid");
                        if (removerUuid == null) {
                            removerUuid = "";
                        }
                        String removerIgn = rs.getString("removed_by_name");
                        if (removerIgn == null) {
                            removerIgn = getLastKnownUsernameFromUUID(removerUuid);
                            removedTime = null;
                        }

                        ign = getLastKnownUsernameFromUUID(uuid);

                        addToUsersTable(ign, uuid);

//                        System.out.println("[Importer] imported mute #" + id + ". Ign: " + ign + ". UUID: " + uuid + ". Reason: " + reason + ". Punisher Ign: " + punisherIgn + ". Punisher UUID: " + punisher_uuid + "" +
//                                ". Active: " + active + ". Time: " + time + ". Until: " + until + ". IP: " + ip + ". Remover Ign: " + removerIgn + ". Remover UUID: " + removerUuid + ". Remover Time: " + removedTime);

                        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                            if (until == null) {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `mutes` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `mutes` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            } else {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `mutes` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `mutes` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            }
                        }
                    }
                    sender.sendMessage(Chat.format("&aImported " + count + " mutes from litebans!"));
                    importKicks(sender);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void importBans(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection litebans = datasource.getConnection()) {
                    System.out.println("[ThePunisher] starting import of bans from litebans!");
                    PreparedStatement pst = litebans.prepareStatement("SELECT * FROM `litebans_bans`");
                    ResultSet rs = pst.executeQuery();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        int id = rs.getInt("id");
                        String ign;
                        String uuid = rs.getString("uuid");
                        if (uuid == null || uuid.equals("#undefined#") || uuid.equals("#offline#")) {
                            uuid = "";
                        }
                        String reason = rs.getString("reason").replaceAll("'", "").replaceAll("\\\\", "");
                        String punisherIgn = rs.getString("banned_by_name");
                        String punisher_uuid = rs.getString("banned_by_uuid");
                        if (punisherIgn == null ||punisherIgn.equals("#undefined#")) {
                            punisherIgn = getLastKnownUsernameFromUUID(punisher_uuid);
                        }
                        int active = rs.getInt("active");
                        Timestamp time = new Timestamp(rs.getLong("time"));
                        if (time.getTime() < Timestamp.valueOf("1970-01-10 00:10:00").getTime()) {
                            time = Timestamp.valueOf("1970-01-10 00:10:00");
                        }
                        Timestamp until = null;
                        Timestamp removedTime = rs.getTimestamp("removed_by_date");
                        if (rs.getLong("until") > 0) {
                            until = new Timestamp(rs.getLong("until"));
                            if (until.getTime() > Timestamp.valueOf("2038-01-10 00:00:00").getTime()) {
                                until = Timestamp.valueOf("2038-01-10 00:10:00");
                            }
                        }
                        String ip = rs.getString("ip");
                        if (ip == null || ip.equals("127.0.0.1") || ip.contains("#")) {
                            ip = "";
                        }
                        String removerUuid = rs.getString("removed_by_uuid");
                        if (removerUuid == null) {
                            removerUuid = "";
                        }
                        String removerIgn = rs.getString("removed_by_name");
                        if (removerIgn == null) {
                            removerIgn = getLastKnownUsernameFromUUID(removerUuid);
                            removedTime = null;
                        }

                        ign = getLastKnownUsernameFromUUID(uuid);

                        addToUsersTable(ign, uuid);

//                        System.out.println("[Importer] imported ban #" + id + ". Ign: " + ign + ". UUID: " + uuid + ". Reason: " + reason + ". Punisher Ign: " + punisherIgn + ". Punisher UUID: " + punisher_uuid + "" +
//                                ". Active: " + active + ". Time: " + time + ". Until: " + until + ". IP: " + ip + ". Remover Ign: " + removerIgn + ". Remover UUID: " + removerUuid + ". Remover Time: " + removedTime);

                        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
                            if (until == null) {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', NULL, '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            } else {
                                if (removedTime == null) {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', NULL)");
                                    pst3.execute();
                                } else {
                                    PreparedStatement pst3 = con.prepareStatement("INSERT INTO `bans` (`id`, `ign`, `uuid`, `reason`, `punisher_ign`, `punisher_uuid`, `active`, `time`, `until`, `ip`, " +
                                            "`remover_ign`, `remover_uuid`, `removed_time`) VALUES (NULL, '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisherIgn + "', '" + punisher_uuid + "', '" + active + "" +
                                            "', '" + time + "', '" + until + "', '" + ip + "', '" + removerIgn + "', '" + removerUuid + "', '" + removedTime + "')");
                                    pst3.execute();
                                }
                            }
                        }
                    }
                    sender.sendMessage(Chat.format("&aImported " + count + " bans from litebans!"));
                    importMutes(sender);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addToUsersTable(String ign, String uuid) {
        try (Connection con = plugin.getSql().getDatasource().getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT * FROM `users` WHERE `uuid` = '" + uuid + "'");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                pst = con.prepareStatement("UPDATE `users` SET `ign`='" + ign + "' WHERE `uuid` = '" + uuid + "'");
                pst.execute();
            } else {
                pst = con.prepareStatement("INSERT INTO `users` (`id`, `ign`, `uuid`, `ip`) VALUES (NULL, '" + ign + "', '" + uuid + "', '')");
                pst.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getLastKnownUsernameFromUUID(String uuid) {
        try (Connection con = datasource.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT * FROM `litebans_history` WHERE `uuid` = '" + uuid + "' ORDER BY `date` DESC");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String ign = rs.getString("name");
                if (!ign.isEmpty() && !ign.contains("/") && !ign.contains("&")) {
                    return ign;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void closeConnections() {
        if (this.datasource != null) {
            this.datasource.close();
        }
    }
}
