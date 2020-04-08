package com.dnyferguson.thepunisher.database;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.interfaces.UserIsPunishedCallback;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;

public class MySQL {

    private ThePunisher plugin;
    private HikariDataSource datasource;

    public MySQL(ThePunisher plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfig();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + cfg.getString("mysql.ip") + ":" + cfg.getString("mysql.port") + "/" + cfg.getString("mysql.database"));
        config.setUsername(cfg.getString("mysql.username"));
        config.setPassword(cfg.getString("mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.datasource = new HikariDataSource(config);

        createTables(cfg.getString("mysql.database"));
    }

    private void createTables(String database) {
        try (Connection con = datasource.getConnection()) {
            // Create users table
            PreparedStatement pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`users` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `ip` VARCHAR(50) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create logins table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`logins` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `ip` VARCHAR(50) NOT NULL , `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create bypass ban
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`bypass_ban` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `active` BOOLEAN NOT NULL DEFAULT TRUE , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP , `remover_ign` VARCHAR(16) NOT NULL , `remover_uuid` VARCHAR(36) NOT NULL , `removed_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create bypass mute
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`bypass_mute` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `active` BOOLEAN NOT NULL DEFAULT TRUE , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP , `remover_ign` VARCHAR(16) NOT NULL , `remover_uuid` VARCHAR(36) NOT NULL , `removed_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create admin logs table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`admin_logs` ( `id` INT NOT NULL AUTO_INCREMENT , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `action` VARCHAR(1024) NOT NULL , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create punishments table (merging all punishments into one table)
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`punishments` ( `id` INT NOT NULL AUTO_INCREMENT , `type` VARCHAR(16) NOT NULL, `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `reason` VARCHAR(1024) NOT NULL , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `active` BOOLEAN NOT NULL DEFAULT TRUE , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP , `until` TIMESTAMP NULL , `ip` VARCHAR(50) NOT NULL , `remover_ign` VARCHAR(16) NOT NULL , `remover_uuid` VARCHAR(36) NOT NULL , `removed_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getTargetType(String target) {
        String targetType = "ign";

        if (target.contains("-")) {
            targetType = "uuid";
        }

        if (target.contains(".")) {
            targetType = "ip";
        }

        return targetType;
    }

    public void isPlayerPunished(String target, String punishmentType, UserIsPunishedCallback callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    String targetType = getTargetType(target);
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `punishments` WHERE `" + targetType + "` = '" + target + "' AND `active` = 1 AND `type` = '" + punishmentType + "'");
                    ResultSet rs = pst.executeQuery();
                    callback.onPlayerIsPunished(rs.next());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                callback.onPlayerIsPunished(false);
            }
        });
    }

    public void getResultAsync(String stmt, FindResultCallback callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    PreparedStatement pst = con.prepareStatement(stmt);
                    ResultSet rs = pst.executeQuery();
                    callback.onQueryDone(rs);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getResultSync(String stmt, FindResultCallback callback) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    PreparedStatement pst = con.prepareStatement(stmt);
                    ResultSet rs = pst.executeQuery();
                    callback.onQueryDone(rs);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void executeStatementSync(String stmt) {
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    PreparedStatement pst = con.prepareStatement(stmt);
                    pst.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void executeStatementAsync(String stmt) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    PreparedStatement pst = con.prepareStatement(stmt);
                    pst.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void convertToNewDataStructure() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Connection con = datasource.getConnection()) {
                    int count = 0;

                    // Merge bans
                    System.out.println("[ThePunisher] starting merging of bans");
                    PreparedStatement pst = con.prepareStatement("SELECT * FROM `bans`");
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        String ign = rs.getString("ign");
                        String uuid = rs.getString("uuid");
                        String reason = rs.getString("reason");
                        String punisher_ign = rs.getString("punisher_ign");
                        String punisher_uuid = rs.getString("punisher_uuid");
                        int active = rs.getInt("active");
                        Timestamp time = null;
                        if (rs.getTimestamp("time") != null) {
                            time = rs.getTimestamp("time");
                        }
                        Timestamp until = null;
                        if (rs.getTimestamp("until") != null) {
                            until = rs.getTimestamp("until");
                        }
                        String ip = rs.getString("ip");
                        String remover_ign = rs.getString("remover_ign");
                        String remover_uuid = rs.getString("remover_uuid");
                        Timestamp removed_time = null;
                        if (rs.getTimestamp("removed_time") != null) {
                            removed_time = rs.getTimestamp("removed_time");
                        }

                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`," +
                                " `punisher_uuid`, `active`, `time`, `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES" +
                                " (NULL, 'ban', '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisher_ign + "', '" + punisher_uuid + "'," +
                                " '" + active + "', " + (time == null ? "NULL" : "'" + time + "'") + ", " + (until == null ? "NULL" : "'" + time + "'") + ", '" + ip + "', '" + remover_ign + "', '" + remover_uuid + "'," +
                                " " + (removed_time == null ? "NULL" : "'" + time + "'") + ")");
                        pst.execute();
                        count++;
                    }
                    System.out.println("[ThePunisher] merged " + count + " bans into the punishments table");

                    // Merge mutes
                    System.out.println("[ThePunisher] starting merging of mutes");
                    count = 0;
                    pst = con.prepareStatement("SELECT * FROM `mutes`");
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        String ign = rs.getString("ign");
                        String uuid = rs.getString("uuid");
                        String reason = rs.getString("reason");
                        String punisher_ign = rs.getString("punisher_ign");
                        String punisher_uuid = rs.getString("punisher_uuid");
                        int active = rs.getInt("active");
                        Timestamp time = null;
                        if (rs.getTimestamp("time") != null) {
                            time = rs.getTimestamp("time");
                        }
                        Timestamp until = null;
                        if (rs.getTimestamp("until") != null) {
                            until = rs.getTimestamp("until");
                        }
                        String ip = rs.getString("ip");
                        String remover_ign = rs.getString("remover_ign");
                        String remover_uuid = rs.getString("remover_uuid");
                        Timestamp removed_time = null;
                        if (rs.getTimestamp("removed_time") != null) {
                            removed_time = rs.getTimestamp("removed_time");
                        }

                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`," +
                                " `punisher_uuid`, `active`, `time`, `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES" +
                                " (NULL, 'mute', '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisher_ign + "', '" + punisher_uuid + "'," +
                                " '" + active + "', " + (time == null ? "NULL" : "'" + time + "'") + ", " + (until == null ? "NULL" : "'" + time + "'") + ", '" + ip + "', '" + remover_ign + "', '" + remover_uuid + "'," +
                                " " + (removed_time == null ? "NULL" : "'" + time + "'") + ")");
                        pst.execute();
                        count++;
                    }
                    System.out.println("[ThePunisher] merged " + count + " mutes into the punishments table");

                    // Merge kicks
                    System.out.println("[ThePunisher] starting merging of kicks");
                    count = 0;
                    pst = con.prepareStatement("SELECT * FROM `kicks`");
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        String ign = rs.getString("ign");
                        String uuid = rs.getString("uuid");
                        String reason = rs.getString("reason");
                        String punisher_ign = rs.getString("punisher_ign");
                        String punisher_uuid = rs.getString("punisher_uuid");
                        Timestamp time = null;
                        if (rs.getTimestamp("time") != null) {
                            time = rs.getTimestamp("time");
                        }

                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`," +
                                " `punisher_uuid`, `active`, `time`, `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES" +
                                " (NULL, 'kick', '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisher_ign + "', '" + punisher_uuid + "'," +
                                " '1', " + (time == null ? "NULL" : "'" + time + "'") + ", NULL"  + ", '', '', ''," +
                                " NULL)");
                        pst.execute();
                        count++;
                    }
                    System.out.println("[ThePunisher] merged " + count + " kicks into the punishments table");

                    // Merge warns
                    System.out.println("[ThePunisher] starting merging of warns");
                    count = 0;
                    pst = con.prepareStatement("SELECT * FROM `warns`");
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        String ign = rs.getString("ign");
                        String uuid = rs.getString("uuid");
                        String reason = rs.getString("reason");
                        String punisher_ign = rs.getString("punisher_ign");
                        String punisher_uuid = rs.getString("punisher_uuid");
                        int active = rs.getInt("active");
                        Timestamp time = null;
                        if (rs.getTimestamp("time") != null) {
                            time = rs.getTimestamp("time");
                        }
                        Timestamp until = null;
                        if (rs.getTimestamp("until") != null) {
                            until = rs.getTimestamp("until");
                        }
                        String ip = rs.getString("ip");
                        String remover_ign = rs.getString("remover_ign");
                        String remover_uuid = rs.getString("remover_uuid");
                        Timestamp removed_time = null;
                        if (rs.getTimestamp("removed_time") != null) {
                            removed_time = rs.getTimestamp("removed_time");
                        }

                        pst = con.prepareStatement("INSERT INTO `punishments` (`id`, `type`, `ign`, `uuid`, `reason`, `punisher_ign`," +
                                " `punisher_uuid`, `active`, `time`, `until`, `ip`, `remover_ign`, `remover_uuid`, `removed_time`) VALUES" +
                                " (NULL, 'warn', '" + ign + "', '" + uuid + "', '" + reason + "', '" + punisher_ign + "', '" + punisher_uuid + "'," +
                                " '" + active + "', " + (time == null ? "NULL" : "'" + time + "'") + ", " + (until == null ? "NULL" : "'" + time + "'") + ", '" + ip + "', '" + remover_ign + "', '" + remover_uuid + "'," +
                                " " + (removed_time == null ? "NULL" : "'" + time + "'") + ")");
                        pst.execute();
                        count++;
                    }
                    System.out.println("[ThePunisher] merged " + count + " warns into the punishments table");
                    System.out.println("[ThePunisher] merging complete!");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void closeConnections() {
        this.datasource.close();
    }

    public HikariDataSource getDatasource() {
        return datasource;
    }
}
