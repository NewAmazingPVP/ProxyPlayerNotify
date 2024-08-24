package bpn.velocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "proxyplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP", version = "2.2.2", url = "https://www.spigotmc.org/resources/bungeeplayernotify.108035/", dependencies = {
        @Dependency(id = "luckperms", optional = true)
})
public class VelocityPlayerNotify {

    private Toml config;
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private final Set<UUID> messageToggles = new HashSet<>();
    private Set<String> disabledServers;
    private Set<String> privateServers;
    private Set<String> limboServers;
    private boolean noVanishNotifications;
    private final ConcurrentHashMap<UUID, String> playerLastServer = new ConcurrentHashMap<>();
    private final Map<String, String> serverNames = new HashMap<>();

    @Inject
    public VelocityPlayerNotify(ProxyServer proxy, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        config = loadConfig(dataDirectory);
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        config = loadConfig(dataDirectory);
        metricsFactory.make(this, 18744);
        proxy.getCommandManager().register("reloadProxyNotifyConfig", new ReloadPlugin());
        proxy.getCommandManager().register("togglemessages", new ToggleMessagesCommand());
        disabledServers = new HashSet<>(config.getList("disabled_servers"));
        privateServers = new HashSet<>(config.getList("private_servers"));
        limboServers = new HashSet<>(config.getList("limbo_servers"));
        noVanishNotifications = config.getBoolean("disable_vanish_notifications");
        loadServerNames();
    }

    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        return new Toml().read(file);
    }

    private void loadServerNames() {
        serverNames.clear();
        Toml serverNamesConfig = config.getTable("ServerNames");
        if (serverNamesConfig != null) {
            for (Map.Entry<String, Object> entry : serverNamesConfig.entrySet()) {
                serverNames.put(entry.getKey().toLowerCase(), entry.getValue().toString());
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        proxy.getScheduler().buildTask(this, () -> {
            if (player.isActive()) {
                String server = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(null);
                if (server != null) {
                    config = loadConfig(dataDirectory);
                    loadServerNames();
                    if(limboServers != null && limboServers.contains(server.toLowerCase())){

                    } else {
                        sendMessage("join_message", player, server, null);
                    }
                    playerLastServer.put(player.getUniqueId(), server);
                }
            }
        }).delay(1, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        config = loadConfig(dataDirectory);
        loadServerNames();
        if (event.getPreviousServer().isPresent()) {
            Player player = event.getPlayer();
            String lastServer = event.getPreviousServer().get().getServerInfo().getName();
            String currentServer = event.getServer().getServerInfo().getName();
            if(limboServers != null && currentServer != null && limboServers.contains(currentServer.toLowerCase())){
                sendMessage("leave_message", player, null, lastServer);
            } else if (limboServers != null && lastServer != null && limboServers.contains(lastServer.toLowerCase())){
                sendMessage("join_message", player, currentServer, null);
            } else {
                sendMessage("switch_message", player, currentServer, lastServer);
            }
            playerLastServer.put(player.getUniqueId(), currentServer);
        } else {
            //sendMessage("switch_message", event.getPlayer(), event.getServer().getServerInfo().getName(), "");
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLeave(DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CONFLICTING_LOGIN &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE) {
            config = loadConfig(dataDirectory);
            loadServerNames();
            Player player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            if(limboServers != null && lastServer != null && limboServers.contains(lastServer.toLowerCase())){

            } else {
                sendMessage("leave_message", player, null, lastServer);
            }
        }
    }

    public void sendMessage(String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        config = loadConfig(dataDirectory);
        loadServerNames();

        if (connectedServer != null && privateServers != null && privateServers.contains(connectedServer.toLowerCase())) {
            return;
        }

        if (disconnectedServer != null && privateServers != null && privateServers.contains(disconnectedServer.toLowerCase())) {
            return;
        }

        if(noVanishNotifications && VelocityVanishAPI.isInvisible(targetPlayer)){
            return;
        }

        //remove permission.permissions in config.yml
        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(type, targetPlayer, connectedServer, disconnectedServer);
                }
            } else {
                sendFormattedMessage(type, targetPlayer, connectedServer, disconnectedServer);
            }
        } else {
            sendFormattedMessage(type, targetPlayer, connectedServer, disconnectedServer);
        }
    }

    private void sendFormattedMessage(String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        String finalMessage = config.getString(type).replace("%player%", targetPlayer.getUsername());
        if (finalMessage.isEmpty()) {
            return;
        }
        if (type.equals("switch_message") || type.equals("join_message") || type.equals("leave_message")) {
            if (connectedServer != null) {
                finalMessage = finalMessage.replace("%server%", getFriendlyServerName(connectedServer));
            }
            if (disconnectedServer != null) {
                finalMessage = finalMessage.replace("%last_server%", getFriendlyServerName(disconnectedServer));
            }
        }
        try {
            if (this.proxy.getPluginManager().getPlugin("luckperms").isPresent()) {
                if (finalMessage.contains("%lp_prefix%")) {
                    String prefix = LuckPermsProvider.get().getUserManager().getUser(targetPlayer.getUniqueId()).getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                    }
                }
                if (finalMessage.contains("%lp_suffix%")) {
                    String suffix = LuckPermsProvider.get().getUserManager().getUser(targetPlayer.getUniqueId()).getCachedData().getMetaData().getSuffix();
                    if (suffix != null) {
                        finalMessage = finalMessage.replace("%lp_suffix%", suffix);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .build();

        Component translatedComponent = serializer.deserialize(finalMessage);
        for (Player pl : proxy.getAllPlayers()) {
            if (!messageToggles.contains(pl.getUniqueId())) {
                if (pl.getCurrentServer().isPresent() && disabledServers != null) {
                    if (!disabledServers.contains(pl.getCurrentServer().get().getServerInfo().getName().toLowerCase())) {
                        if (config.getBoolean("permission.permissions")) {
                            if(config.getBoolean("permission.hide_message")) {
                                if(pl.hasPermission("ppn.view"))
                                    pl.sendMessage(translatedComponent);
                            }
                            else {
                                pl.sendMessage(translatedComponent);
                            }
                        } else {
                            pl.sendMessage(translatedComponent);
                        }
                    }
                } else {
                    if (config.getBoolean("permission.permissions")) {
                        if(config.getBoolean("permission.hide_message")) {
                            if(pl.hasPermission("ppn.view"))
                                pl.sendMessage(translatedComponent);
                        }
                        else {
                            pl.sendMessage(translatedComponent);
                        }
                    } else {
                        pl.sendMessage(translatedComponent);
                    }
                }
            }
        }
    }

    private String getFriendlyServerName(String serverName) {
        return serverNames.getOrDefault(serverName.toLowerCase(), serverName);
    }

    private class ReloadPlugin implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            config = loadConfig(dataDirectory);
            disabledServers = new HashSet<>(config.getList("disabled_servers"));
            privateServers = new HashSet<>(config.getList("private_servers"));
            loadServerNames();
            proxy.getConsoleCommandSource().sendMessage(Component.text("Reload done"));
        }
    }

    private class ToggleMessagesCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (source instanceof Player) {
                Player player = (Player) source;
                if (messageToggles.contains(player.getUniqueId())) {
                    messageToggles.remove(player.getUniqueId());
                    player.sendMessage(Component.text("Message notifications toggled on"));
                } else {
                    messageToggles.add(player.getUniqueId());
                    player.sendMessage(Component.text("Message notifications toggled off"));
                }
            } else {
                source.sendMessage(Component.text("Only players can use this command."));
            }
        }
    }
}
