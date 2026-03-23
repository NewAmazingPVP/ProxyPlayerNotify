package ppn.bungeecord;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ppn.Webhook;
import ppn.bungeecord.utils.MessageSender;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EventListener implements Listener {

    private final BungeePlayerNotify plugin;
    private final Map<UUID, String> playerLastServer = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerLastNonLimboServer = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingLimboLeave = new ConcurrentHashMap<>();
    private final Map<UUID, PendingJoin> pendingJoins = new ConcurrentHashMap<>();

    public EventListener(BungeePlayerNotify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (plugin.getConfig().getBoolean("join_last_server")) {
            String lastServer = getStoredLastServer(player);
            if (lastServer != null) {
                ServerInfo target = plugin.getProxy().getServerInfo(lastServer);
                if (target != null) {
                    event.setTarget(target);
                }
            }
        }

        boolean firstJoin = !plugin.getPlayerData().getBoolean("players." + player.getUniqueId() + ".joined");
        if (firstJoin) {
            plugin.getPlayerData().setOption("players." + player.getUniqueId() + ".joined", true);
        }
        pendingJoins.put(player.getUniqueId(), new PendingJoin(firstJoin));
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String currentServer = event.getServer().getInfo().getName();
        if (currentServer == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PendingJoin pendingJoin = pendingJoins.get(uuid);
        String previousServer = playerLastServer.get(uuid);
        if (!isLimboServer(currentServer)) {
            playerLastNonLimboServer.put(uuid, currentServer);
        } else if (previousServer != null && !isLimboServer(previousServer)) {
            playerLastNonLimboServer.put(uuid, previousServer);
        }

        if (pendingJoin != null) {
            handleInitialConnection(player, currentServer, pendingJoin);
            playerLastServer.put(uuid, currentServer);
            return;
        }

        if (previousServer == null || previousServer.equalsIgnoreCase(currentServer)) {
            playerLastServer.put(uuid, currentServer);
            return;
        }

        handleServerTransition(player, currentServer, previousServer);
        playerLastServer.put(uuid, currentServer);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        pendingJoins.remove(uuid);

        String lastServer = playerLastServer.remove(uuid);
        String lastNonLimboServer = playerLastNonLimboServer.remove(uuid);
        String pendingLimboLeaveServer = pendingLimboLeave.remove(uuid);
        if (lastServer == null && player.getServer() != null && player.getServer().getInfo() != null) {
            lastServer = player.getServer().getInfo().getName();
        }
        if (lastServer == null) {
            lastServer = lastNonLimboServer;
        }
        if (lastServer == null) {
            plugin.removeRecentLeaveMessage(uuid);
            return;
        }
        if (isLimboServer(lastServer)) {
            if (pendingLimboLeaveServer != null) {
                if (plugin.getConfig().getBoolean("join_last_server")) {
                    plugin.getPlayerData().setOption("players." + uuid + ".lastServer", pendingLimboLeaveServer);
                }
                MessageSender.sendMessage(plugin, "leave_message", player, null, pendingLimboLeaveServer);
                sendLeaveWebhook(player, pendingLimboLeaveServer);
                plugin.removeRecentLeaveMessage(uuid);
                return;
            }
            if (lastNonLimboServer != null && plugin.getConfig().getBoolean("join_last_server")) {
                plugin.getPlayerData().setOption("players." + uuid + ".lastServer", lastNonLimboServer);
            }
            plugin.removeRecentLeaveMessage(uuid);
            return;
        }
        if (lastServer != null && plugin.getConfig().getBoolean("join_last_server")) {
            plugin.getPlayerData().setOption("players." + uuid + ".lastServer", lastServer);
        }

        MessageSender.sendMessage(plugin, "leave_message", player, null, lastServer);
        sendLeaveWebhook(player, lastServer);
        plugin.removeRecentLeaveMessage(uuid);
    }

    private void handleInitialConnection(ProxiedPlayer player, String currentServer, PendingJoin pendingJoin) {
        if (isLimboServer(currentServer)) {
            return;
        }

        pendingJoins.remove(player.getUniqueId());
        scheduleJoinSequence(player, currentServer, pendingJoin.firstJoin());
    }

    private void handleServerTransition(ProxiedPlayer player, String currentServer, String previousServer) {
        boolean currentIsLimbo = isLimboServer(currentServer);
        boolean previousIsLimbo = isLimboServer(previousServer);

        if (currentIsLimbo && previousIsLimbo) {
            return;
        }
        if (currentIsLimbo) {
            UUID uuid = player.getUniqueId();
            pendingLimboLeave.put(uuid, previousServer);
            scheduleSwitchDelay(() -> {
                if (!player.isConnected()) {
                    return;
                }
                String currentLiveServer = resolveCurrentServer(player);
                if (currentLiveServer != null && !isLimboServer(currentLiveServer)) {
                    pendingLimboLeave.remove(uuid);
                    return;
                }
                String leaveServer = pendingLimboLeave.remove(uuid);
                if (leaveServer == null) {
                    return;
                }
                MessageSender.sendMessage(plugin, "leave_message", player, null, leaveServer);
                sendLeaveWebhook(player, leaveServer);
            });
            return;
        }
        if (previousIsLimbo) {
            PendingJoin pendingJoin = pendingJoins.remove(player.getUniqueId());
            if (pendingJoin != null) {
                scheduleJoinSequence(player, currentServer, pendingJoin.firstJoin());
                return;
            }
            scheduleSwitchDelay(() -> {
                if (!player.isConnected()) {
                    return;
                }
                MessageSender.sendPrivateMessage(plugin, "join_private_message", player, currentServer);
                MessageSender.sendMessage(plugin, "join_message", player, currentServer, null);
                sendJoinWebhook(player, currentServer);
            });
            return;
        }

        scheduleSwitchDelay(() -> {
            if (!player.isConnected()) {
                return;
            }
            MessageSender.sendMessage(plugin, "switch_message", player, currentServer, previousServer);
            sendSwitchWebhook(player, currentServer, previousServer);
        });
    }

    private void scheduleJoinSequence(ProxiedPlayer player, String currentServer, boolean firstJoin) {
        String publicMessageType = firstJoin ? "first_join_message" : "join_message";
        String privateMessageType = firstJoin ? "first_join_private_message" : "join_private_message";
        long publicDelayMs = (firstJoin ? plugin.getConfig().getLong("first_join_message_delay") : plugin.getConfig().getLong("join_message_delay")) * 50L;
        long privateDelayMs = (firstJoin ? plugin.getConfig().getLong("first_join_private_message_delay") : plugin.getConfig().getLong("join_private_message_delay")) * 50L;

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected()) {
                return;
            }
            MessageSender.sendMessage(plugin, publicMessageType, player, currentServer, null);
            sendJoinWebhook(player, currentServer);
        }, publicDelayMs, TimeUnit.MILLISECONDS);

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            if (!player.isConnected()) {
                return;
            }
            MessageSender.sendPrivateMessage(plugin, privateMessageType, player, currentServer);
        }, privateDelayMs, TimeUnit.MILLISECONDS);
    }

    private void scheduleSwitchDelay(Runnable action) {
        long delayMs = plugin.getConfig().getLong("switch_message_delay") * 50L;
        plugin.getProxy().getScheduler().schedule(plugin, action, delayMs, TimeUnit.MILLISECONDS);
    }

    private String getStoredLastServer(ProxiedPlayer player) {
        Object last = plugin.getPlayerData().getOption("players." + player.getUniqueId() + ".lastServer");
        if (last == null) {
            last = plugin.getConfig().getOption("players." + player.getUniqueId() + ".lastServer");
        }
        return last != null ? last.toString() : null;
    }

    private boolean isLimboServer(String server) {
        return server != null && plugin.getLimboServers() != null && plugin.getLimboServers().contains(server.toLowerCase());
    }

    private String resolveCurrentServer(ProxiedPlayer player) {
        if (player.getServer() == null || player.getServer().getInfo() == null) {
            return null;
        }
        return player.getServer().getInfo().getName();
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

        message = plugin.resolveLuckPermsPlaceholders(message, player);

        int color = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed = plugin.getConfig().getBoolean("webhook.use_embed");
        String url = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (formatted == null || formatted.trim().isEmpty()) {
                return;
            }
            CompletableFuture.runAsync(() -> {
                Webhook.WebhookResult result = useEmbed ? Webhook.sendEmbed(url, formatted, color) : Webhook.send(url, formatted);
                if (!result.isSuccess() && result.getStatusCode() != 0) {
                    plugin.getLogger().warning("Webhook join send failed (" + result.getStatusCode() + "): " + result.getError());
                }
            });
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
        message = plugin.resolveLuckPermsPlaceholders(message, player);
        int color2 = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed2 = plugin.getConfig().getBoolean("webhook.use_embed");
        String url2 = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (formatted == null || formatted.trim().isEmpty()) {
                return;
            }
            CompletableFuture.runAsync(() -> {
                Webhook.WebhookResult result = useEmbed2 ? Webhook.sendEmbed(url2, formatted, color2) : Webhook.send(url2, formatted);
                if (!result.isSuccess() && result.getStatusCode() != 0) {
                    plugin.getLogger().warning("Webhook switch send failed (" + result.getStatusCode() + "): " + result.getError());
                }
            });
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
        message = plugin.resolveLuckPermsPlaceholders(message, player);
        int color3 = plugin.getConfig().getInt("webhook.embed_color");
        boolean useEmbed3 = plugin.getConfig().getBoolean("webhook.use_embed");
        String url3 = plugin.getConfig().getString("webhook.url");
        plugin.getPlaceholderHandler().format(message, player.getUniqueId()).thenAccept(formatted -> {
            if (formatted == null || formatted.trim().isEmpty()) {
                return;
            }
            CompletableFuture.runAsync(() -> {
                Webhook.WebhookResult result = useEmbed3 ? Webhook.sendEmbed(url3, formatted, color3) : Webhook.send(url3, formatted);
                if (!result.isSuccess() && result.getStatusCode() != 0) {
                    plugin.getLogger().warning("Webhook leave send failed (" + result.getStatusCode() + "): " + result.getError());
                }
            });
        });
    }

    private record PendingJoin(boolean firstJoin) {
    }
}
