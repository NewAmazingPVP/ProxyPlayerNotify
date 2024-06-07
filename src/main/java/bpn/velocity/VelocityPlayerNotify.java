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
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "proxyplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP", version = "8.0", url = "https://www.spigotmc.org/resources/bungeeplayernotify.108035/", dependencies = {
        @Dependency(id = "luckperms", optional = true)
})
public class VelocityPlayerNotify {

    private Toml config;
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private Set<UUID> messageToggles = new HashSet<>();
    private Set<String> disabledServers;
    private ConcurrentHashMap<UUID, String> playerLastServer = new ConcurrentHashMap<>();

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
        proxy.getCommandManager().register("reloadProxyNotifyConfig", new reloadPlugin());
        proxy.getCommandManager().register("togglemessages", new ToggleMessagesCommand());
        disabledServers = new HashSet<>(config.getList("disabledServers"));
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

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        proxy.getScheduler().buildTask(this, () -> {
            if (player.isActive()) {
                String server = player.getCurrentServer().get().getServerInfo().getName();
                config = loadConfig(dataDirectory);
                sendMessage("join_message", player, server, null);
                playerLastServer.put(player.getUniqueId(), server);
            }
        }).delay(1, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        config = loadConfig(dataDirectory);
        if(event.getPreviousServer().isPresent()) {
            Player player = event.getPlayer();
            String lastServer = event.getPreviousServer().get().getServerInfo().getName();
            String currentServer = event.getServer().getServerInfo().getName();
            sendMessage("switch_message", player, currentServer, lastServer);
            playerLastServer.put(player.getUniqueId(), currentServer);
        } else {
            //sendMessage("switch_message", event.getPlayer(), event.getServer().getServerInfo().getName(), "");
        }

    }

    @Subscribe(order = PostOrder.LAST)
    public void onLeave(DisconnectEvent event) {
        if(event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CONFLICTING_LOGIN &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE) {
            config = loadConfig(dataDirectory);
            Player player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            sendMessage("leave_message", player, null, lastServer);
        }
    }

    public void sendMessage(String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        config = loadConfig(dataDirectory);

        if (connectedServer != null && disabledServers.contains(connectedServer.toLowerCase())) {
            return;
        }

        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(type, targetPlayer, connectedServer, disconnectedServer);
                }
            } else if (config.getBoolean("permission.hide_message")) {
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
                finalMessage = finalMessage.replace("%server%", connectedServer);
            }
            if (disconnectedServer != null) {
                finalMessage = finalMessage.replace("%last_server%", disconnectedServer);
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
            }
        } catch (Exception ignored) {
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);
        finalMessage = finalMessage.replace("&", "§");
        Component translatedComponent = Component.text(finalMessage);
        for (Player pl : proxy.getAllPlayers()) {
            if (!messageToggles.contains(pl.getUniqueId())) {
                pl.sendMessage(translatedComponent);
            }
        }
    }

    private class reloadPlugin implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            config = loadConfig(dataDirectory);
            disabledServers = new HashSet<>(config.getList("disabledServers"));
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
