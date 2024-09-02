package bpn.bungeecord;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;
    private LuckPerms luckPerms;
    private Map<String, String> serverNames;
    private final HashSet<UUID> playerToggle = new HashSet<>();
    private final Map<UUID, String> playerLastServer = new HashMap<>();
    private Set<String> disabledServers;
    private Set<String> privateServers;
    private Set<String> limboServers;
    private boolean noVanishNotifications;
    private final ArrayList<ProxiedPlayer> validPlayers = new ArrayList<>();
    private static final Pattern HEX_REGEX = Pattern.compile("&#([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])", Pattern.CASE_INSENSITIVE);

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
        limboServers = new HashSet<>(config.getStringList("LimboServers"));

        noVanishNotifications = config.getBoolean("disable_vanish_notifications");

        getProxy().getPluginManager().registerListener(this, this);
        if (config.getString("join_message").contains("%lp_prefix%") || config.getString("switch_message").contains("%lp_prefix%") || config.getString("leave_message").contains("%lp_prefix%")
                || config.getString("join_message").contains("%lp_suffix%") || config.getString("switch_message").contains("%lp_suffix%") || config.getString("leave_message").contains("%lp_suffix%")) {
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
                if (limboServers != null && server != null && limboServers.contains(server.toLowerCase())) {
                    return;
                }
                if (config.getString("join_private_message") != null && !config.getString("join_private_message").isEmpty()) {
                    sendPrivateMessage("join_private_message", player, server);
                }
                sendMessage("join_message", player, server, null);
                playerLastServer.put(player.getUniqueId(), server);
            }
        }, 1, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (event.getFrom() == null) return;
        String lastServer = event.getFrom().getName();
        if (player.isConnected()) {
            String currentServer = player.getServer().getInfo().getName();
            saveDefaultConfig();
            loadConfig();
            if (limboServers != null && currentServer != null && limboServers.contains(currentServer.toLowerCase())) {
                sendMessage("leave_message", player, null, lastServer);
            } else if (limboServers != null && lastServer != null && limboServers.contains(lastServer.toLowerCase())) {
                if (config.getString("join_private_message") != null && !config.getString("join_private_message").isEmpty()) {
                    sendPrivateMessage("join_private_message", player, currentServer);
                }
                sendMessage("join_message", player, currentServer, null);
            } else {
                sendMessage("switch_message", player, currentServer, lastServer);
            }
            playerLastServer.put(player.getUniqueId(), currentServer);
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (validPlayers.remove(event.getPlayer())) {
            ProxiedPlayer player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            saveDefaultConfig();
            loadConfig();
            if (limboServers != null && lastServer != null && limboServers.contains(lastServer.toLowerCase())) {
                return;
            }
            if(lastServer == null) return;
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

        if (noVanishNotifications && BungeeVanishAPI.isInvisible(targetPlayer)) {
            return;
        }

        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(type, targetPlayer, server, lastServer);
                }
            } else {
                sendFormattedMessage(type, targetPlayer, server, lastServer);
            }
        } else {
            sendFormattedMessage(type, targetPlayer, server, lastServer);
        }
    }

    public void sendPrivateMessage(String type, ProxiedPlayer targetPlayer, String server) {
        saveDefaultConfig();
        loadConfig();

        String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
        if (finalMessage.isEmpty()) {
            return;
        }
        if (server != null) {
            finalMessage = finalMessage.replace("%server%", server);
        }
        if (finalMessage.contains("%lp_prefix%")) {
            User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
        }
        if (finalMessage.contains("%lp_suffix%")) {
            User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String suffix = user.getCachedData().getMetaData().getSuffix();
            if (suffix != null) {
                finalMessage = finalMessage.replace("%lp_suffix%", suffix);
            }
        }
        finalMessage = finalMessage.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        sendMessageToPlayer(targetPlayer, finalMessage);
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
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
        }
        if (finalMessage.contains("%lp_suffix%")) {
            User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String suffix = user.getCachedData().getMetaData().getSuffix();
            if (suffix != null) {
                finalMessage = finalMessage.replace("%lp_suffix%", suffix);
            }
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);

        for (ProxiedPlayer pl : getProxy().getPlayers()) {
            if (!playerToggle.contains(pl.getUniqueId())) {
                if (pl.getServer() != null && disabledServers != null) {
                    if (!disabledServers.contains(pl.getServer().getInfo().getName().toLowerCase())) {
                        if (config.getBoolean("permission.permissions")) {
                            if (config.getBoolean("permission.hide_message")) {
                                if (pl.hasPermission("ppn.view")) {
                                    sendMessageToPlayer(pl, finalMessage);
                                }
                            } else {
                                sendMessageToPlayer(pl, finalMessage);
                            }
                        } else {
                            sendMessageToPlayer(pl, finalMessage);
                        }
                    }
                } else {
                    if (config.getBoolean("permission.permissions")) {
                        if (config.getBoolean("permission.hide_message")) {
                            if (pl.hasPermission("ppn.view")) {
                                sendMessageToPlayer(pl, finalMessage);
                            }
                        } else {
                            sendMessageToPlayer(pl, finalMessage);
                        }
                    } else {
                        sendMessageToPlayer(pl, finalMessage);
                    }
                }
            }
        }
    }

    private void sendMessageToPlayer(ProxiedPlayer player, String message) {
        message = replace(message);
        message = message.replace("&", "§");
        message = ChatColor.translateAlternateColorCodes('§', message);
        String[] lines = message.split("\n");
        for (String line : lines) {
            player.sendMessage(line);
        }
    }

    public String replace(String s) {
        return HEX_REGEX.matcher(s).replaceAll("&x&$1&$2&$3&$4&$5&$6");
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

        private final BungeePlayerNotify plugin;

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
