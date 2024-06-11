package bpn.bungeecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.event.EventPriority;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;
    private LuckPerms luckPerms;
    private Map<String, String> serverNames;
    private HashSet<UUID> playerToggle = new HashSet<>();
    private Map<UUID, String> playerLastServer = new HashMap<>();
    private Set<String> disabledServers;
    private Set<String> privateServers;
    private ArrayList<ProxiedPlayer> validPlayers = new ArrayList<>();

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
        privateServers = new HashSet<>(config.getStringList("PrivateServers"));

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        getProxy().getScheduler().schedule(this, () -> {
            if (player.isConnected()) {
                validPlayers.add(event.getPlayer());
                saveDefaultConfig();
                loadConfig();
                String server = player.getServer().getInfo().getName();
                sendMessage("join_message", player, server, null);
                playerLastServer.put(player.getUniqueId(), server);
            }
        }, 1, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String lastServer = event.getFrom().getName();
        if (player.isConnected()) {
            String currentServer = player.getServer().getInfo().getName();
            saveDefaultConfig();
            loadConfig();
            sendMessage("switch_message", player, currentServer, lastServer);
            playerLastServer.put(player.getUniqueId(), currentServer);
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if(validPlayers.remove(event.getPlayer())) {
            ProxiedPlayer player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            saveDefaultConfig();
            loadConfig();
            sendMessage("leave_message", player, null, lastServer);
        }

    }

    public void sendMessage(String type, ProxiedPlayer targetPlayer, String server, String lastServer) {
        saveDefaultConfig();
        loadConfig();

        if (server != null && privateServers != null && privateServers.contains(server.toLowerCase())) {
            return;
        }

        if (lastServer != null && privateServers != null && privateServers.contains(lastServer.toLowerCase())) {
            return;
        }

        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(type, targetPlayer, server, lastServer);
                }
            } else if (config.getBoolean("permission.hide_message")) {
                sendFormattedMessage(type, targetPlayer, server, lastServer);
            }
        } else {
            sendFormattedMessage(type, targetPlayer, server, lastServer);
        }
    }

    private void sendFormattedMessage(String type, ProxiedPlayer targetPlayer, String server, String lastServer) {
        String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
        if (finalMessage.isEmpty()) {
            return;
        }
        if (type.equals("switch_message") || type.equals("join_message") || type.equals("leave_message")) {
            if (server != null) {
                finalMessage = finalMessage.replace("%server%", server);
            }
            if (lastServer != null) {
                finalMessage = finalMessage.replace("%last_server%", lastServer);
            }
        }
        if (finalMessage.contains("%lp_prefix%")) {
            User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                prefix = prefix.replace("&", "§");
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);
        finalMessage = finalMessage.replace("&", "§");
        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', finalMessage));
        for (ProxiedPlayer pl : getProxy().getPlayers()) {
            if (!playerToggle.contains(pl.getUniqueId())) {
                if(pl.getServer() != null && disabledServers != null)
                {
                    if(!disabledServers.contains(pl.getServer().getInfo().getName().toLowerCase())){
                        pl.sendMessage(message);
                    }
                } else {
                    pl.sendMessage(message);
                }
            }
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
                        privateServers = new HashSet<>(config.getStringList("PrivateServers"));
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
                privateServers = new HashSet<>(config.getStringList("PrivateServers"));
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
