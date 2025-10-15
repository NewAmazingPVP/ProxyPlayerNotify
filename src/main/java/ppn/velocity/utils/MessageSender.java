package ppn.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ppn.velocity.VelocityPlayerNotify;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MessageSender {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static String getMessage(VelocityPlayerNotify plugin, String path) {
        Object val = plugin.getConfig().getOption(path);
        if (val instanceof Iterable<?>) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (Iterable<?>) val) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(o.toString());
            }
            return sb.toString();
        }
        return val != null ? val.toString() : "";
    }

    public static void sendMessage(VelocityPlayerNotify plugin, String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        if (connectedServer != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(connectedServer.toLowerCase())) {
            return;
        }

        if (plugin.getDisabledPlayers().contains(targetPlayer.getUsername().toLowerCase())) {
            return;
        }

        if (disconnectedServer != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(disconnectedServer.toLowerCase())) {
            return;
        }

        if (plugin.isNoVanishNotifications() && isVanishAvailable(plugin)) {
            try {
                if (VelocityVanishAPI.isInvisible(targetPlayer)) {
                    return;
                }
            } catch (NoClassDefFoundError ignored) {
                // Vanish API not present; ignore
            }
        }

        if (plugin.getConfig().getBoolean("permission.permissions")) {
            if (plugin.getConfig().getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(plugin, type, targetPlayer, connectedServer, disconnectedServer);
                }
            } else {
                sendFormattedMessage(plugin, type, targetPlayer, connectedServer, disconnectedServer);
            }
        } else {
            sendFormattedMessage(plugin, type, targetPlayer, connectedServer, disconnectedServer);
        }
    }

    public static void sendPrivateMessage(VelocityPlayerNotify plugin, String type, Player targetPlayer, String connectedServer) {
        String finalMessage = getMessage(plugin, type).replace("%player%", targetPlayer.getUsername());
        if (finalMessage.trim().isEmpty()) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(targetPlayer.getUsername().toLowerCase())) {
            return;
        }
        if (connectedServer != null) {
            finalMessage = finalMessage.replace("%server%", plugin.getServerNames().getOrDefault(connectedServer.toLowerCase(), connectedServer));
        }
        finalMessage = plugin.resolveLuckPermsPlaceholders(finalMessage, targetPlayer);
        finalMessage = finalMessage.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        plugin.getPlaceholderHandler().format(finalMessage, targetPlayer.getUniqueId())
                .thenAccept(formatted -> sendMessageToPlayer(plugin, targetPlayer, formatted));
    }

    private static void sendFormattedMessage(VelocityPlayerNotify plugin, String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        String finalMessage;
        if (type.equals("leave_message")) {
            finalMessage = plugin.getRecentLeaveMessage(targetPlayer.getUniqueId());
            if (finalMessage == null || finalMessage.trim().isEmpty()) {
                finalMessage = getMessage(plugin, type);
            }
        } else {
            finalMessage = getMessage(plugin, type);
        }
        finalMessage = finalMessage.replace("%player%", targetPlayer.getUsername());
        if (finalMessage.trim().isEmpty()) {
            return;
        }
        if (type.equals("switch_message") || type.equals("join_message") || type.equals("leave_message")) {
            if (connectedServer != null) {
                finalMessage = finalMessage.replace("%server%", plugin.getServerNames().getOrDefault(connectedServer.toLowerCase(), connectedServer));
            }
            if (disconnectedServer != null) {
                finalMessage = finalMessage.replace("%last_server%", plugin.getServerNames().getOrDefault(disconnectedServer.toLowerCase(), disconnectedServer));
            }
        }
        finalMessage = plugin.resolveLuckPermsPlaceholders(finalMessage, targetPlayer);
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);
        plugin.getPlaceholderHandler().format(finalMessage, targetPlayer.getUniqueId())
                .thenAccept(formatted -> sendMessageToAll(plugin, formatted));
    }

    private static void sendMessageToAll(VelocityPlayerNotify plugin, String message) {
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (!plugin.getMessageToggles().contains(player.getUniqueId())) {
                if (player.getCurrentServer().isPresent() && plugin.getDisabledServers() != null) {
                    if (!plugin.getDisabledServers().contains(player.getCurrentServer().get().getServerInfo().getName().toLowerCase())) {
                        if (plugin.getConfig().getBoolean("permission.permissions")) {
                            if (plugin.getConfig().getBoolean("permission.hide_message")) {
                                if (player.hasPermission("ppn.view")) {
                                    sendMessageToPlayer(plugin, player, message);
                                }
                            } else {
                                sendMessageToPlayer(plugin, player, message);
                            }
                        } else {
                            sendMessageToPlayer(plugin, player, message);
                        }
                    }
                } else {
                    if (plugin.getConfig().getBoolean("permission.permissions")) {
                        if (plugin.getConfig().getBoolean("permission.hide_message")) {
                            if (player.hasPermission("ppn.view")) {
                                sendMessageToPlayer(plugin, player, message);
                            }
                        } else {
                            sendMessageToPlayer(plugin, player, message);
                        }
                    } else {
                        sendMessageToPlayer(plugin, player, message);
                    }
                }
            }
        }
    }

    private static void sendMessageToPlayer(VelocityPlayerNotify plugin, Player player, String message) {
        String[] lines = message.split("\n");
        if (plugin.getConfig().getBoolean("use_minimessage")) {
            for (String line : lines) {
                player.sendMessage(MINI_MESSAGE.deserialize(line));
            }
        } else {
            for (String line : lines) {
                player.sendMessage(LEGACY_SERIALIZER.deserialize(line));
            }
        }
    }

    private static boolean isVanishAvailable(VelocityPlayerNotify plugin) {
        return plugin.getProxy().getPluginManager().getPlugin("premiumvanish").isPresent()
                || plugin.getProxy().getPluginManager().getPlugin("supervanish").isPresent();
    }
}
