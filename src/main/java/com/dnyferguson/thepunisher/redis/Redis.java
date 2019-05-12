package com.dnyferguson.thepunisher.redis;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.UUID;

public class Redis {

    private ThePunisher plugin;
    private JedisPool jedisPool;

    public Redis(ThePunisher plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();
        String ip = config.getString("redis.ip");
        int port = Integer.valueOf(config.getString("redis.port"));
        String password = config.getString("redis.password");

        JedisPoolConfig poolConfig = buildPoolConfig();
        if (password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, ip, port, 2000);
        } else {
            jedisPool = new JedisPool(poolConfig, ip, port, 2000, password);
        }

        subscribe();
    }

    public void sendMessage(String message) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.publish("punisher", message);
                }
            }
        });
    }

    private void subscribe() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            System.out.println("Received jedis message: " + message);
                            String[] args = message.split(" ");
                            if (args[0].equals("ban")) {
                                String target = args[1];
                                String reason = args[2];
                                Player player = plugin.getServer().getPlayer(UUID.fromString(target));
                                if (player != null) {
                                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            player.kickPlayer(Chat.format("&cYou have been banned!\n&7Reason: " + reason));
                                        }
                                    });
                                }
                            }
                            if (args[0].equals("kick")) {
                                String target = args[1];
                                String reason = args[2];
                                Player player = plugin.getServer().getPlayer(UUID.fromString(target));
                                if (player != null) {
                                    plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            player.kickPlayer(Chat.format("&cYou have been kicked!\n&7Reason: " + reason));
                                        }
                                    });
                                }
                            }
                        }
                    }, "punisher");
                }
            }
        });
    }

    public void closeConnections() {
        this.jedisPool.destroy();
    }

    private JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(32);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(4);
        poolConfig.setMaxWaitMillis(1000);
        return poolConfig;
    }
}
