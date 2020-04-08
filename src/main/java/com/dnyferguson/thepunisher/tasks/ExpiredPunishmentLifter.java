package com.dnyferguson.thepunisher.tasks;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.database.FindResultCallback;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpiredPunishmentLifter {

    public ExpiredPunishmentLifter(ThePunisher plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                System.out.println("[ThePunisher] Looking for expired punishments...");
                plugin.getSql().getResultAsync("SELECT * FROM `punishments` WHERE `active` = '1' AND `until` < CURRENT_TIMESTAMP", new FindResultCallback() {
                    @Override
                    public void onQueryDone(ResultSet result) throws SQLException {
                        int count = 0;
                        while (result.next()) {
                            int id = result.getInt("id");
                            plugin.getSql().executeStatementAsync("UPDATE `punishments` SET `active`='0', `remover_ign`='#expired', `remover_uuid`='', `removed_time`=CURRENT_TIMESTAMP WHERE `id` = '" + id + "'");
                            count++;
                        }
                        System.out.println("[ThePunisher] Expired " + count + " punishments.");
                    }
                });
            }
        }, 100, 1200);
    }
}
