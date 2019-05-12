package com.dnyferguson.thepunisher.events;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChat implements Listener {

    private ThePunisher plugin;

    public PlayerChat(ThePunisher plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (plugin.getMutedPlayers().contains(player.getUniqueId().toString())) {
            e.setCancelled(true);
            player.sendMessage(Chat.format("&cYou can\'t chat because you are muted."));
        }
    }
}
