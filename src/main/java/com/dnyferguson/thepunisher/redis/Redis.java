package com.dnyferguson.thepunisher.redis;

import com.dnyferguson.thepunisher.ThePunisher;
import com.dnyferguson.thepunisher.utils.Chat;
import org.bukkit.Bukkit;
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
    private JedisPubSub pubSub;

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
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
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
                    jedis.subscribe(pubSub = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            System.out.println("Received jedis message: " + message);
                            String[] args = message.split("/");
                            if (args[0].equals("ban")) {
                                String target = args[1];
                                String reason = args[2];
                                try {
                                    Player player;
                                    if (target.contains("-")) {
                                        player = plugin.getServer().getPlayer(UUID.fromString(target));
                                    } else {
                                        player = plugin.getServer().getPlayer(target);
                                    }
                                    if (player != null) {
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                            @Override
                                            public void run() {
                                                player.kickPlayer(Chat.format("&cYou have been banned!\n&7Reason: " + reason));
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (args[0].equals("kick")) {
                                String target = args[1];
                                String reason = args[2];
                                try {
                                    Player player;
                                    if (target.contains("-")) {
                                        player = plugin.getServer().getPlayer(UUID.fromString(target));
                                    } else {
                                        player = plugin.getServer().getPlayer(target);
                                    }
                                    if (player != null) {
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                            @Override
                                            public void run() {
                                                player.kickPlayer(Chat.format("&cYou have been kicked!\n&7Reason: " + reason));
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (args[0].equals("notify")) {
                                String target = args[1];
                                String msg = args[2];
                                try {
                                    Player player;
                                    if (target.contains("-")) {
                                        player = plugin.getServer().getPlayer(UUID.fromString(target));
                                    } else {
                                        player = plugin.getServer().getPlayer(target);
                                    }
                                    if (player != null) {
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                            @Override
                                            public void run() {
                                                player.sendMessage(Chat.format(msg));
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (args[0].equals("mute")) {
                                String target = args[1];
                                String reason = args[2];
                                try {
                                    Player player;
                                    if (target.contains("-")) {
                                        player = plugin.getServer().getPlayer(UUID.fromString(target));
                                    } else {
                                        player = plugin.getServer().getPlayer(target);
                                    }
                                    if (player != null) {
                                        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                            @Override
                                            public void run() {
                                                plugin.getMutedPlayers().add(target);
                                                if (args.length == 3) {
                                                    player.sendMessage(Chat.format("&cYou have been permanently muted for &7" + reason + "&c."));
                                                } else {
                                                    String until = args[3];
                                                    player.sendMessage(Chat.format("&cYou have been temporarily muted for &7" + reason + "&c. Expires: &7" + until));
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (args[0].equals("alertplayers")) {
                                String msg = args[1];
                                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        for (Player player : Bukkit.getOnlinePlayers()) {
                                            player.sendMessage(Chat.format(msg));
                                        }
                                    }
                                });
                            }
                            if (args[0].equals("alertstaff")) {
                                String msg = args[1];
                                plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        for (Player player : Bukkit.getOnlinePlayers()) {
                                            if (player.hasPermission("thepunisher.notify")) {
                                                player.sendMessage(Chat.format(msg));
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }, "punisher");
                }
            }
        });
    }

    public void closeConnections() {
        pubSub.unsubscribe();
        jedisPool.close();
    }

    private JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }
}
