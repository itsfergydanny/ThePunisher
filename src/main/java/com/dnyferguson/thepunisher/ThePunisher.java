package com.dnyferguson.thepunisher;

import com.dnyferguson.thepunisher.commands.BanCommand;
import com.dnyferguson.thepunisher.commands.PunisherCommand;
import com.dnyferguson.thepunisher.commands.UnbanCommand;
import com.dnyferguson.thepunisher.database.MySQL;
import com.dnyferguson.thepunisher.events.PlayerLogin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ThePunisher extends JavaPlugin {

    private MySQL sql;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        sql = new MySQL(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerLogin(this), this);

        getCommand("punisher").setExecutor(new PunisherCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MySQL getSql() {
        return sql;
    }
}
