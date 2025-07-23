package ppn.bungeecord;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import ppn.Webhook;
import ppn.bungeecord.utils.MessageSender;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EventListener implements Listener {

    private final BungeePlayerNotify plugin;
    private final Map<UUID, String> playerLastServer = new HashMap<>();
    private final ArrayList<ProxiedPlayer> validPlayers = new ArrayList<>();

    public EventListener(BungeePlayerNotify plugin) {
        this.plugin = plugin;
    }

    @EventHandler()
    public void onRejoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (plugin.getConfig().getBoolean("join_last_server")) {
            String lastServer = (String) plugin.getConfig().getOption("players." + player.getUniqueId() + ".lastServer");
            if (lastServer != null) {
                player.connect(plugin.getProxy().getServerInfo(lastServer));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        boolean firstJoin = !plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".joined");
        if (firstJoin) {
            plugin.getConfig().setOption("players." + player.getUniqueId() + ".joined", true);
        }
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (player.isConnected()) {
                validPlayers.add(player);
                String server = player.getServer().getInfo().getName();
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }
                MessageSender.sendMessage(plugin, firstJoin ? "first_join_message" : "join_message", player, server, null);
                sendJoinWebhook(player, server);
                playerLastServer.put(player.getUniqueId(), server);
            }
        }, plugin.getConfig().getLong("join_message_delay") * 50, TimeUnit.MILLISECONDS);

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (player.isConnected()) {
                String server = player.getServer().getInfo().getName();
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
            }
        }, plugin.getConfig().getLong("join_private_message_delay") * 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (event.getFrom() == null) return;
            String lastServer = event.getFrom().getName();
            if (player.isConnected()) {
                String currentServer = player.getServer().getInfo().getName();
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
                playerLastServer.put(player.getUniqueId(), currentServer);
            }
        }, plugin.getConfig().getLong("switch_message_delay") * 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (validPlayers.remove(event.getPlayer())) {
            ProxiedPlayer player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                return;
            }
            if (lastServer != null && plugin.getConfig().getBoolean("join_last_server")) {
                plugin.getConfig().setOption("players." + player.getUniqueId() + ".lastServer", lastServer);
            }
            MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
        }
    }

    private void sendJoinWebhook(ProxiedPlayer player, String server) {
        if (!plugin.getConfig().getBoolean("webhook.enabled")) {
            return;
        }
        if (server != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(server.toLowerCase())) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(player.getName().toLowerCase())) {
            return;
        }
        if (plugin.isNoVanishNotifications() && BungeeVanishAPI.isInvisible(player)) {
            return;
        }
        String message = plugin.getConfig().getString("webhook.message").replace("%player%", player.getName());
        if (server != null) {
            message = message.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
        }
        message = message.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted ->
                Webhook.send(plugin.getConfig().getString("webhook.url"), formatted));
    }
}