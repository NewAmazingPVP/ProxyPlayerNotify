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
            Object last = plugin.getPlayerData().getOption("players." + player.getUniqueId() + ".lastServer");
            if (last == null) {
                last = plugin.getConfig().getOption("players." + player.getUniqueId() + ".lastServer");
            }
            String lastServer = last != null ? last.toString() : null;
            if (lastServer != null) {
                player.connect(plugin.getProxy().getServerInfo(lastServer));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        boolean firstJoin = !plugin.getPlayerData().getBoolean("players." + player.getUniqueId() + ".joined");
        if (firstJoin) {
            plugin.getPlayerData().setOption("players." + player.getUniqueId() + ".joined", true);
        }
        long joinDelay = firstJoin ? plugin.getConfig().getLong("first_join_message_delay") : plugin.getConfig().getLong("join_message_delay");
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (player.isConnected()) {
                if (player.getServer() == null || player.getServer().getInfo() == null) {
                    return;
                }
                validPlayers.add(player);
                String server = player.getServer().getInfo().getName();
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }
                MessageSender.sendMessage(plugin, firstJoin ? "first_join_message" : "join_message", player, server, null);
                sendJoinWebhook(player, server);
                playerLastServer.put(player.getUniqueId(), server);
            }
        }, joinDelay * 50, TimeUnit.MILLISECONDS);

        long privateDelay = firstJoin ? plugin.getConfig().getLong("first_join_private_message_delay") : plugin.getConfig().getLong("join_private_message_delay");
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (player.isConnected()) {
                if (player.getServer() == null || player.getServer().getInfo() == null) {
                    return;
                }
                String server = player.getServer().getInfo().getName();
                if (plugin.getLimboServers() != null && server != null && plugin.getLimboServers().contains(server.toLowerCase())) {
                    return;
                }
                MessageSender.sendPrivateMessage(plugin, firstJoin ? "first_join_private_message" : "join_private_message", player, server);
            }
        }, privateDelay * 50, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (event.getFrom() == null) return;
            String lastServer = event.getFrom().getName();
            if (player.isConnected()) {
                if (player.getServer() == null || player.getServer().getInfo() == null) {
                    return;
                }
                String currentServer = player.getServer().getInfo().getName();
                if (plugin.getLimboServers() != null && currentServer != null && lastServer != null && plugin.getLimboServers().contains(currentServer.toLowerCase())) {
                    MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
                    sendLeaveWebhook(player, lastServer);
                } else if (plugin.getLimboServers() != null && lastServer != null && plugin.getLimboServers().contains(lastServer.toLowerCase())) {
                    if (!plugin.getConfig().getStringList("join_private_message").isEmpty()) {
                        MessageSender.sendPrivateMessage(plugin, "join_private_message", player, currentServer);
                    }
                    MessageSender.sendMessage(plugin, "join_message", player, currentServer, null);
                    sendJoinWebhook(player, currentServer);
                } else {
                    MessageSender.sendMessage(plugin, "switch_message", player, currentServer, lastServer);
                    sendSwitchWebhook(player, currentServer, lastServer);
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
                plugin.getPlayerData().setOption("players." + player.getUniqueId() + ".lastServer", lastServer);
            }
            MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
            sendLeaveWebhook(player, lastServer);
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
        if (plugin.isNoVanishNotifications()) {
            try {
                if (BungeeVanishAPI.isInvisible(player)) {
                    return;
                }
            } catch (NoClassDefFoundError ignored) {
                // Vanish API not present; ignore
            }
        }
        String message = plugin.getConfig().getString("webhook.join_message");
        if (message == null || message.trim().isEmpty()) {
            message = plugin.getConfig().getString("webhook.message");
        }
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        message = message.replace("%player%", player.getName());
        if (server != null) {
            message = message.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
        }
        message = message.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        boolean papiBridge = plugin.getProxy().getPluginManager().getPlugin("PAPIProxyBridge") != null;
        if (papiBridge) {
            message = message.replace("%lp_prefix%", "%luckperms_prefix%").replace("%lp_suffix%", "%luckperms_suffix%");
        } else if (plugin.getLuckPerms() != null) {
            try {
                if (message.contains("%lp_prefix%")) {
                    String prefix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getPrefix();
                    if (prefix != null) message = message.replace("%lp_prefix%", prefix);
                }
                if (message.contains("%lp_suffix%")) {
                    String suffix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getSuffix();
                    if (suffix != null) message = message.replace("%lp_suffix%", suffix);
                }
            } catch (Exception ignored) {}
        }

        int color = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed = plugin.getConfig().getBoolean("webhook.use_embed");
        String url = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (useEmbed) {
                Webhook.sendEmbed(url, formatted, color);
            } else {
                Webhook.send(url, formatted);
            }
        });
    }

    private void sendSwitchWebhook(ProxiedPlayer player, String server, String lastServer) {
        if (!plugin.getConfig().getBoolean("webhook.enabled")) {
            return;
        }
        if (server != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(server.toLowerCase())) {
            return;
        }
        if (lastServer != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(lastServer.toLowerCase())) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(player.getName().toLowerCase())) {
            return;
        }
        if (plugin.isNoVanishNotifications()) {
            try {
                if (BungeeVanishAPI.isInvisible(player)) {
                    return;
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }
        String message = plugin.getConfig().getString("webhook.switch_message");
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        message = message.replace("%player%", player.getName());
        if (server != null) {
            message = message.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
        }
        if (lastServer != null) {
            message = message.replace("%last_server%", plugin.getServerNames().getOrDefault(lastServer.toLowerCase(), lastServer));
        }
        message = message.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        boolean papiBridge2 = plugin.getProxy().getPluginManager().getPlugin("PAPIProxyBridge") != null;
        if (papiBridge2) {
            message = message.replace("%lp_prefix%", "%luckperms_prefix%").replace("%lp_suffix%", "%luckperms_suffix%");
        } else if (plugin.getLuckPerms() != null) {
            try {
                if (message.contains("%lp_prefix%")) {
                    String prefix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getPrefix();
                    if (prefix != null) message = message.replace("%lp_prefix%", prefix);
                }
                if (message.contains("%lp_suffix%")) {
                    String suffix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getSuffix();
                    if (suffix != null) message = message.replace("%lp_suffix%", suffix);
                }
            } catch (Exception ignored) {}
        }
        int color2 = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed2 = plugin.getConfig().getBoolean("webhook.use_embed");
        String url2 = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (useEmbed2) {
                Webhook.sendEmbed(url2, formatted, color2);
            } else {
                Webhook.send(url2, formatted);
            }
        });
    }

    private void sendLeaveWebhook(ProxiedPlayer player, String lastServer) {
        if (!plugin.getConfig().getBoolean("webhook.enabled")) {
            return;
        }
        if (lastServer != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(lastServer.toLowerCase())) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(player.getName().toLowerCase())) {
            return;
        }
        if (plugin.isNoVanishNotifications()) {
            try {
                if (BungeeVanishAPI.isInvisible(player)) {
                    return;
                }
            } catch (NoClassDefFoundError ignored) {
            }
        }
        String message = plugin.getConfig().getString("webhook.leave_message");
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        message = message.replace("%player%", player.getName());
        if (lastServer != null) {
            message = message.replace("%last_server%", plugin.getServerNames().getOrDefault(lastServer.toLowerCase(), lastServer));
        }
        message = message.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        boolean papiBridge3 = plugin.getProxy().getPluginManager().getPlugin("PAPIProxyBridge") != null;
        if (papiBridge3) {
            message = message.replace("%lp_prefix%", "%luckperms_prefix%").replace("%lp_suffix%", "%luckperms_suffix%");
        } else if (plugin.getLuckPerms() != null) {
            try {
                if (message.contains("%lp_prefix%")) {
                    String prefix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getPrefix();
                    if (prefix != null) message = message.replace("%lp_prefix%", prefix);
                }
                if (message.contains("%lp_suffix%")) {
                    String suffix = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(player).getCachedData().getMetaData().getSuffix();
                    if (suffix != null) message = message.replace("%lp_suffix%", suffix);
                }
            } catch (Exception ignored) {}
        }
        int color3 = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed3 = plugin.getConfig().getBoolean("webhook.use_embed");
        String url3 = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (useEmbed3) {
                Webhook.sendEmbed(url3, formatted, color3);
            } else {
                Webhook.send(url3, formatted);
            }
        });
    }
}
