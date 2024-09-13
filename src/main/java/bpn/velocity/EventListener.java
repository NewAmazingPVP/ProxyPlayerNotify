package bpn.velocity;

import bpn.velocity.utils.MessageSender;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.concurrent.TimeUnit;

public class EventListener {

    private final VelocityPlayerNotify plugin;

    public EventListener(VelocityPlayerNotify plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (player.isActive()) {
                String server = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(null);
                if (server != null) {
                    plugin.setConfig(ConfigLoader.loadConfig(plugin.getDataDirectory()));
                    ConfigLoader.loadServerNames(plugin.getConfig(), plugin.getServerNames());
                    if (plugin.getLimboServers() != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                        return;
                    }
                    if (plugin.getConfig().getString("join_private_message") != null && !plugin.getConfig().getString("join_private_message").isEmpty()) {
                        MessageSender.sendPrivateMessage(plugin, "join_private_message", player, server);
                    }
                    MessageSender.sendMessage(plugin, "join_message", player, server, null);
                    plugin.getPlayerLastServer().put(player.getUniqueId(), server);
                }
            }
        }).delay(1, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        plugin.setConfig(ConfigLoader.loadConfig(plugin.getDataDirectory()));
        ConfigLoader.loadServerNames(plugin.getConfig(), plugin.getServerNames());
        if (event.getPreviousServer().isPresent()) {
            Player player = event.getPlayer();
            String lastServer = event.getPreviousServer().get().getServerInfo().getName();
            String currentServer = event.getServer().getServerInfo().getName();
            if (plugin.getLimboServers() != null && currentServer != null && plugin.getLimboServers().contains(currentServer.toLowerCase())) {
                MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
            } else if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                if (plugin.getConfig().getString("join_private_message") != null && !plugin.getConfig().getString("join_private_message").isEmpty()) {
                    MessageSender.sendPrivateMessage(plugin, "join_private_message", player, currentServer);
                }
                MessageSender.sendMessage(plugin, "join_message", player, currentServer, null);
            } else {
                MessageSender.sendMessage(plugin, "switch_message", player, currentServer, lastServer);
            }
            plugin.getPlayerLastServer().put(player.getUniqueId(), currentServer);
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLeave(DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CONFLICTING_LOGIN &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE) {
            plugin.setConfig(ConfigLoader.loadConfig(plugin.getDataDirectory()));
            ConfigLoader.loadServerNames(plugin.getConfig(), plugin.getServerNames());
            Player player = event.getPlayer();
            String lastServer = plugin.getPlayerLastServer().remove(player.getUniqueId());
            if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                return;
            }
            if (lastServer == null) return;
            MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
        }
    }
}