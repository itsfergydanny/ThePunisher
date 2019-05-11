package com.dnyferguson.thepunisher;

import com.dnyferguson.thepunisher.commands.*;
import com.dnyferguson.thepunisher.database.MySQL;
import com.dnyferguson.thepunisher.events.PlayerChat;
import com.dnyferguson.thepunisher.events.PlayerLogin;
import com.dnyferguson.thepunisher.events.PlayerSendCommand;
import com.dnyferguson.thepunisher.interfaces.UserIsPunishedCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public final class ThePunisher extends JavaPlugin {

    private MySQL sql;
    private Set<String> mutedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        sql = new MySQL(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerLogin(this), this);
        pm.registerEvents(new PlayerChat(this), this);
        pm.registerEvents(new PlayerSendCommand(this), this);

        getCommand("punisher").setExecutor(new PunisherCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("checkban").setExecutor(new CheckbanCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("checkmute").setExecutor(new CheckmuteCommand(this));
        getCommand("alts").setExecutor(new AltsCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("bypassban").setExecutor(new BypassBanCommand(this));
        getCommand("bypassmute").setExecutor(new BypassMuteCommand(this));
        getCommand("unbypassban").setExecutor(new UnbypassBanCommand(this));
        getCommand("unbypassmute").setExecutor(new UnbypassMuteCommand(this));
        getCommand("iphistory").setExecutor(new IpHistoryCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));

        // Iterate thru all online players to apply any mutes on reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            sql.isPlayerPunished(uuid, "mutes", new UserIsPunishedCallback() {
                @Override
                public void onPlayerIsPunished(boolean result) {
                    if (result) {
                        mutedPlayers.add(uuid);
                    }
                }
            });
        }
    }

    @Override
    public void onDisable() {
        sql.closeConnections();
    }

    public MySQL getSql() {
        return sql;
    }

    public Set<String> getMutedPlayers() {
        return mutedPlayers;
    }
}
