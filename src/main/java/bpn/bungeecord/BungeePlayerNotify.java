package bpn.bungeecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private HashSet<UUID> playerToggle = new HashSet<>();
    private Set<String> disabledServers;

    @Override
    public void onEnable() {
        new Metrics(this, 18703);
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        loadConfig();

        serverNames = new HashMap<>();
        Configuration section = config.getSection("ServerNames");
        for (String server : section.getKeys()) {
            serverNames.put(server.toLowerCase(), section.getString(server));
        }

        disabledServers = new HashSet<>(config.getStringList("DisabledServers"));

        getProxy().getPluginManager().registerListener(this, this);
        if (config.getString("join_message").contains("%lp_prefix%") || config.getString("switch_message").contains("%lp_prefix%") || config.getString("leave_message").contains("%lp_prefix%")) {
            luckPerms = LuckPermsProvider.get();
        }

        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        getProxy().getPluginManager().registerCommand(this, new ToggleMessagesCommand(this));
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

        if (server != null && disabledServers.contains(server.toLowerCase())) {
            return;
        }

        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                    if (finalMessage.isEmpty()) {
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
                            prefix = prefix.replace("&", "§");
                            finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                        }
                    }
                    if (config.getBoolean("permission.hide_message")) {
                        for (ProxiedPlayer pl : getProxy().getPlayers()) {
                            if (pl.hasPermission("ppn.view") && !playerToggle.contains(pl.getUniqueId())) {
                                TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
                                pl.sendMessage(message);
                            }
                        }
                    } else {
                        for (ProxiedPlayer pl : getProxy().getPlayers()) {
                            if (!playerToggle.contains(pl.getUniqueId())) {
                                TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
                                pl.sendMessage(message);
                            }
                        }
                        //getProxy().broadcast(finalMessage);
                    }
                }
            } else if (config.getBoolean("permission.hide_message")) {
                String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                if (finalMessage.isEmpty()) {
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
                        prefix = prefix.replace("&", "§");
                        finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                    }
                }
                finalMessage = finalMessage.replace("&", "§");
                for (ProxiedPlayer pl : getProxy().getPlayers()) {
                    if (pl.hasPermission("ppn.view") && !playerToggle.contains(pl.getUniqueId())) {
                        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
                        pl.sendMessage(message);
                    }
                }
            }
        } else {
            String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
            if (finalMessage.isEmpty()) {
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
            for (ProxiedPlayer pl : getProxy().getPlayers()) {
                if (!playerToggle.contains(pl.getUniqueId())) {
                    pl.sendMessage(message);
                }
            }
            //TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
            //getProxy().broadcast(message);
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

                        serverNames.clear();
                        Configuration section = config.getSection("ServerNames");
                        for (String server : section.getKeys()) {
                            serverNames.put(server.toLowerCase(), section.getString(server));
                        }

                        disabledServers = new HashSet<>(config.getStringList("DisabledServers"));
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have ppn.reloadProxyNotifyConfig permission to use this command");
                    }
                }
            } else {
                getProxy().broadcast("Reload done");
                saveDefaultConfig();
                loadConfig();

                serverNames.clear();
                Configuration section = config.getSection("ServerNames");
                for (String server : section.getKeys()) {
                    serverNames.put(server.toLowerCase(), section.getString(server));
                }

                disabledServers = new HashSet<>(config.getStringList("DisabledServers"));
            }
        }
    }

    public class ToggleMessagesCommand extends Command {

        private BungeePlayerNotify plugin;

        public ToggleMessagesCommand(BungeePlayerNotify plugin) {
            super("togglemessages");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (sender instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) sender;
                if (playerToggle.contains(player.getUniqueId())) {
                    playerToggle.remove(player.getUniqueId());
                    sender.sendMessage(ChatColor.GREEN + "Message notifications toggled on");
                } else {
                    playerToggle.add(player.getUniqueId());
                    sender.sendMessage(ChatColor.RED + "Message notifications toggled off");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            }
        }
    }
}
