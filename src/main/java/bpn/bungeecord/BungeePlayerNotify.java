package bpn.bungeecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;
    private LuckPerms luckPerms;
    private Map<String, String> serverNames;

    @Override
    public void onEnable() {
        new Metrics(this, 18703);
        // Create the plugin data folder if it does not already exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        loadConfig();

        // Load custom server names from the config.
        serverNames = new HashMap<>();
        Configuration section = config.getSection("ServerNames");
        for (String server : section.getKeys()) {
            serverNames.put(server.toLowerCase(), section.getString(server));
        }

        // Register the plugin's event listener
        getProxy().getPluginManager().registerListener(this, this);
        if (config.getString("join_message").contains("%lp_prefix%") || config.getString("switch_message").contains("%lp_prefix%") || config.getString("leave_message").contains("%lp_prefix%")) {
            luckPerms = LuckPermsProvider.get();
        }

        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
    }

    public void saveDefaultConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig() {
        File file = new File(getDataFolder(), "config.yml");
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        saveDefaultConfig();
        loadConfig();
        sendMessage("join_message", event.getPlayer(), null);
    }

    @EventHandler
    public void onSwitch(ServerConnectedEvent event) {
        saveDefaultConfig();
        loadConfig();
        sendMessage("switch_message", event.getPlayer(), event.getServer().getInfo().getName());
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        saveDefaultConfig();
        loadConfig();
        sendMessage("leave_message", event.getPlayer(), null);
    }

    public void sendMessage(String type, ProxiedPlayer targetPlayer, String server) {
        saveDefaultConfig();
        loadConfig();
        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                    if (finalMessage.equals("")) {
                        return;
                    }
                    if (type.equals("switch_message")) {
                        String customServerName = serverNames.get(server.toLowerCase());
                        if (customServerName != null) {
                            server = customServerName;
                        }
                        finalMessage = finalMessage.replace("%server%", server);
                    }
                    //finalMessage = finalMessage.replace("&", "§");
                    if (finalMessage.contains("%lp_prefix%")) {
                        User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
                        String prefix = user.getCachedData().getMetaData().getPrefix();
                        if (prefix != null) {
                            //prefix = prefix.replace("&", "§");
                            finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                        }
                    }
                    if (config.getBoolean("permission.hide_message")) {
                        for (ProxiedPlayer pl : getProxy().getPlayers()) {
                            if (pl.hasPermission("ppn.view")) {
                                TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
                                pl.sendMessage(message);
                            }
                        }
                    } else {
                        getProxy().broadcast(finalMessage);
                    }
                }
            } else if (config.getBoolean("permission.hide_message")) {
                String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                if (finalMessage.equals("")) {
                    return;
                }
                if (type.equals("switch_message")) {
                    String customServerName = serverNames.get(server.toLowerCase());
                    if (customServerName != null) {
                        server = customServerName;
                    }
                    finalMessage = finalMessage.replace("%server%", server);
                }
                if (finalMessage.contains("%lp_prefix%")) {
                    User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        //prefix = prefix.replace("&", "§");
                        finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                    }
                }
                //finalMessage = finalMessage.replace("&", "§");
                for (ProxiedPlayer pl : getProxy().getPlayers()) {
                    if (pl.hasPermission("ppn.view")) {
                        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
                        pl.sendMessage(message);
                    }
                }
            }
        } else {
            String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
            if (finalMessage.equals("")) {
                return;
            }
            if (type.equals("switch_message")) {
                String customServerName = serverNames.get(server.toLowerCase());
                if (customServerName != null) {
                    server = customServerName;
                }
                finalMessage = finalMessage.replace("%server%", server);
            }
            if (finalMessage.contains("%lp_prefix%")) {
                User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null) {
                    //prefix = prefix.replace("&", "§");
                    finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                }
            }
            //finalMessage = finalMessage.replace("&", "§");
            TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
            getProxy().broadcast(message);
        }
    }

    public class ReloadCommand extends Command {

        public ReloadCommand() {
            super("reloadProxyNotifyConfig");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ProxiedPlayer) {
                if (args.length < 1) {
                    if (sender.hasPermission("ppn.reloadProxyNotifyConfig")) {
                        sender.sendMessage("Reload done");
                        saveDefaultConfig();
                        loadConfig();

                        // Reload custom server names.
                        serverNames.clear();
                        Configuration section = config.getSection("ServerNames");
                        for (String server : section.getKeys()) {
                            serverNames.put(server.toLowerCase(), section.getString(server));
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have ppn.reloadProxyNotifyConfig permission to use this command");
                    }
                }
            } else {
                getProxy().broadcast("Reload done");
                saveDefaultConfig();
                loadConfig();

                // Reload custom server names.
                serverNames.clear();
                Configuration section = config.getSection("ServerNames");
                for (String server : section.getKeys()) {
                    serverNames.put(server.toLowerCase(), section.getString(server));
                }
            }
        }
    }
}
