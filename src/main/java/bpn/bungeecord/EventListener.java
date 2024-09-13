package bpn.bungeecord;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class EventListener implements Listener {

    private final BungeePlayerNotify plugin;
    private final Set<UUID> playerToggle = new HashSet<>();
    private final Map<UUID, String> playerLastServer = new HashMap<>();
    private final ArrayList<ProxiedPlayer> validPlayers = new ArrayList<>();

    public EventListener(BungeePlayerNotify plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (player.isConnected()) {
                validPlayers.add(event.getPlayer());
                plugin.saveDefaultConfig();
                plugin.loadConfig();
                String server = player.getServer().getInfo().getName();
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }
                if (plugin.getConfig().getString("join_private_message") != null && !plugin.getConfig().getString("join_private_message").isEmpty()) {
                    MessageSender.sendPrivateMessage(plugin, "join_private_message", player, server);
                }
                MessageSender.sendMessage(plugin, "join_message", player, server, null);
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
            plugin.saveDefaultConfig();
            plugin.loadConfig();
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
            playerLastServer.put(player.getUniqueId(), currentServer);
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (validPlayers.remove(event.getPlayer())) {
            ProxiedPlayer player = event.getPlayer();
            String lastServer = playerLastServer.remove(player.getUniqueId());
            plugin.saveDefaultConfig();
            plugin.loadConfig();
            if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                return;
            }
            if (lastServer == null) return;
            MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
        }
    }
}