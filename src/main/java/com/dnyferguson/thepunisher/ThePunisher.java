package com.dnyferguson.thepunisher;

import com.dnyferguson.thepunisher.commands.*;
import com.dnyferguson.thepunisher.database.MySQL;
import com.dnyferguson.thepunisher.events.PlayerChat;
import com.dnyferguson.thepunisher.events.PlayerLogin;
import com.dnyferguson.thepunisher.events.PlayerSendCommand;
import com.dnyferguson.thepunisher.interfaces.UserIsPunishedCallback;
import com.dnyferguson.thepunisher.redis.Redis;
import com.dnyferguson.thepunisher.tasks.ExpiredPunishmentLifter;
import com.dnyferguson.thepunisher.utils.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public final class ThePunisher extends JavaPlugin {

    private MySQL sql;
    private Redis redis;
    private Set<String> mutedPlayers = new HashSet<>();
    private ImportCommand importer;
    private ExpiredPunishmentLifter expiredPunishmentLifter;
    private String server;
    private String discordWebhook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        server = getConfig().getString("server");
        discordWebhook = getConfig().getString("discordWebhook");

        sql = new MySQL(this);
        redis = new Redis(this);
        expiredPunishmentLifter = new ExpiredPunishmentLifter(this);

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
        getCommand("staffrollback").setExecutor(new StaffRollbackCommand(this));
        getCommand("unwarn").setExecutor(new UnWarnCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("punishermerge").setExecutor(new MergeCommand(this));
        getCommand("litebansimport").setExecutor(importer = new ImportCommand(this));
        getCommand("clearchat").setExecutor(new ClearChatCommand(this));

        // Iterate thru all online players to apply any mutes on reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            String ip = player.getAddress().getAddress().getHostAddress();
            String uuid = player.getUniqueId().toString();
            sql.isPlayerPunished(ip, "mute", new UserIsPunishedCallback() {
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
        if (sql != null) {
            sql.closeConnections();
        }
        if (redis != null) {
            redis.closeConnections();
        }
        if (importer != null) {
            importer.closeConnections();
        }
    }

    public void logToDiscord(String issuer, String type, String description) {
        getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                try {
                    DiscordWebhook webhook = new DiscordWebhook(discordWebhook);
                    webhook.addEmbed(new DiscordWebhook.EmbedObject()
                            .setColor(Color.RED)
                            .addField("Issuer", issuer, true)
                            .addField("Type/Command", type, true)
                            .addField("Server", server, true)
                            .addField("Description", description, false)
                            .setFooter("Executed at " + timeStamp + " EST", "https://i.imgur.com/pvxwozW.png"));
                    webhook.execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public MySQL getSql() {
        return sql;
    }

    public Redis getRedis() {
        return redis;
    }

    public Set<String> getMutedPlayers() {
        return mutedPlayers;
    }
}
