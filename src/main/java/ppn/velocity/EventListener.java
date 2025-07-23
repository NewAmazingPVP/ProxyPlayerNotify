package ppn.velocity;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import ppn.Webhook;
import ppn.velocity.utils.MessageSender;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final VelocityPlayerNotify plugin;

    public EventListener(VelocityPlayerNotify plugin) {
        this.plugin = plugin;
    }

    @Subscribe()
    public void onRejoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("join_last_server")) {
            String lastServer = (String) plugin.getConfig().getOption("players." + player.getUniqueId() + ".lastServer");
            if (lastServer != null) {
                plugin.getProxy().getServer(lastServer).ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".joined");
        if (firstJoin) {
            plugin.getConfig().setOption("players." + player.getUniqueId() + ".joined", true);
        }
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (player.isActive()) {
                String server = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(null);
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }

                MessageSender.sendMessage(plugin, firstJoin ? "first_join_message" : "join_message", player, server, null);
                sendJoinWebhook(player, server);
                plugin.getPlayerLastServer().put(player.getUniqueId(), server);
            }
        }).delay(plugin.getConfig().getLong("join_message_delay") * 50, TimeUnit.MILLISECONDS).schedule();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (player.isActive()) {
                String server = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(null);
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }

                if (firstJoin) {
                    if (plugin.getConfig().getString("first_join_private_message") != null && !plugin.getConfig().getString("first_join_private_message").isEmpty()) {
                        MessageSender.sendPrivateMessage(plugin, "first_join_private_message", player, server);
                    }
                } else {
                    if (plugin.getConfig().getString("join_private_message") != null && !plugin.getConfig().getString("join_private_message").isEmpty()) {
                        MessageSender.sendPrivateMessage(plugin, "join_private_message", player, server);
                    }
                }
                plugin.getPlayerLastServer().put(player.getUniqueId(), server);
            }
        }).delay(plugin.getConfig().getLong("join_private_message_delay") * 50, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (event.getPreviousServer().isPresent()) {
                Player player = event.getPlayer();
                String lastServer = event.getPreviousServer().get().getServerInfo().getName();
                String currentServer = event.getServer().getServerInfo().getName();
                if (plugin.getLimboServers() != null && currentServer != null && lastServer != null && plugin.getLimboServers().contains(currentServer.toLowerCase())) {
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
        }).delay(plugin.getConfig().getLong("switch_message_delay") * 50, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLeave(DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CONFLICTING_LOGIN &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE &&
                event.getLoginStatus() != DisconnectEvent.LoginStatus.PRE_SERVER_JOIN) {
            Player player = event.getPlayer();
            String lastServer = plugin.getPlayerLastServer().remove(player.getUniqueId());
            if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                return;
            }
            if (lastServer != null && plugin.getConfig().getBoolean("join_last_server")) {
                plugin.getConfig().setOption("players." + player.getUniqueId() + ".lastServer", lastServer);
            }
            MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
        }
    }

    private void sendJoinWebhook(Player player, String server) {
        if (!plugin.getConfig().getBoolean("webhook.enabled")) {
            return;
        }
        if (server != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(server.toLowerCase())) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(player.getUsername().toLowerCase())) {
            return;
        }
        if (plugin.isNoVanishNotifications() && VelocityVanishAPI.isInvisible(player)) {
            return;
        }
        String message = plugin.getConfig().getString("webhook.message").replace("%player%", player.getUsername());
        if (server != null) {
            message = message.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
        }
        message = message.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        plugin.getPlaceholderHandler().format(message, player.getUniqueId())
                .thenAccept(formatted -> Webhook.send(plugin.getConfig().getString("webhook.url"), formatted));
    }
}