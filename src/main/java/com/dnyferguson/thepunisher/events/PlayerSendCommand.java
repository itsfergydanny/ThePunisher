package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;

public class PlayerSendCommand implements Listener {

    private ThePunisher plugin;
    private List<String> blockedCommandsWhileMuted = new ArrayList<>();

    public PlayerSendCommand(ThePunisher plugin) {
        this.plugin = plugin;
        this.blockedCommandsWhileMuted = plugin.getConfig().getStringList("blocked-commands-when-muted");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String command = e.getMessage().split(" ")[0];
        System.out.println("[ThePunisher] command executed: " + command);
        if (blockedCommandsWhileMuted.contains(command)) {
            e.setCancelled(true);
            player.sendMessage(Chat.format("&cYou can\'t use that command because you are muted."));
        }
    }
}
