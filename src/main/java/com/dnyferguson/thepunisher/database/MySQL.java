package com.dnyferguson.thepunisher.database;

import com.dnyferguson.thepunisher.ThePunisher;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQL {
    private HikariDataSource datasource;

    public MySQL(ThePunisher plugin) {
        FileConfiguration cfg = plugin.getConfig();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + cfg.getString("mysql.ip") + ":" + cfg.getString("mysql.port") + "/" + cfg.getString("mysql.database"));
        config.setUsername(cfg.getString("mysql.username"));
        config.setPassword(cfg.getString("mysql.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        datasource = new HikariDataSource(config);

        createTables(cfg.getString("mysql.database"));
    }

    private void createTables(String database) {
        try (Connection con = datasource.getConnection()) {
            // Create users table
            PreparedStatement pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`users` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `ip` VARCHAR(50) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create bans table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`bans` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `reason` VARCHAR(1024) NOT NULL , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `active` BOOLEAN NOT NULL DEFAULT TRUE , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP , `until` TIMESTAMP NULL , `ip` VARCHAR(50) NOT NULL , `remover_ign` VARCHAR(16) NOT NULL , `remover_uuid` VARCHAR(36) NOT NULL , `removed_time` TIMESTAMP NULL, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create mutes table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`mutes` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `reason` VARCHAR(1024) NOT NULL , `punisher_ign` VARCHAR(16) NOT NULL , `punisher_uuid` VARCHAR(36) NOT NULL , `active` BOOLEAN NOT NULL DEFAULT TRUE , `time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP , `until` TIMESTAMP NULL , `ip` VARCHAR(50) NOT NULL , `remover_ign` VARCHAR(16) NOT NULL , `remover_uuid` VARCHAR(36) NOT NULL , `removed_time` TIMESTAMP NULL, PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();

            // Create logins table
            pst = con.prepareStatement("CREATE TABLE IF NOT EXISTS `" + database + "`.`logins` ( `id` INT NOT NULL AUTO_INCREMENT , `ign` VARCHAR(16) NOT NULL , `uuid` VARCHAR(36) NOT NULL , `ip` VARCHAR(50) NOT NULL , `time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
            pst.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HikariDataSource getDatasource() {
        return datasource;
    }
}
