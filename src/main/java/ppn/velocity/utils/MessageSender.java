package ppn.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import de.myzelyam.api.vanish.VelocityVanishAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPermsProvider;
import ppn.velocity.VelocityPlayerNotify;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MessageSender {

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

        if (plugin.isNoVanishNotifications() && VelocityVanishAPI.isInvisible(targetPlayer)) {
            return;
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
        String finalMessage = plugin.getConfig().getString(type).replace("%player%", targetPlayer.getUsername());
        if (finalMessage.isEmpty()) {
            return;
        }
        if (plugin.getDisabledPlayers().contains(targetPlayer.getUsername().toLowerCase())) {
            return;
        }
        if (connectedServer != null) {
            finalMessage = finalMessage.replace("%server%", plugin.getServerNames().getOrDefault(connectedServer.toLowerCase(), connectedServer));
        }
        try {
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
        } catch (Exception ignored) {
        }
        finalMessage = finalMessage.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        plugin.getPlaceholderHandler().format(finalMessage, targetPlayer.getUniqueId())
                .thenAccept(formatted -> sendMessageToAll(plugin, formatted));
    }

    private static void sendFormattedMessage(VelocityPlayerNotify plugin, String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        String finalMessage;
        if (type.equals("leave_message")) {
            finalMessage = plugin.getRecentLeaveMessage(targetPlayer.getUniqueId()) != null ? plugin.getRecentLeaveMessage(targetPlayer.getUniqueId()) : plugin.getConfig().getString(type);
        } else {
            finalMessage = plugin.getConfig().getString(type);
        }
        finalMessage = finalMessage.replace("%player%", targetPlayer.getUsername());
        if (finalMessage.isEmpty()) {
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
        if (plugin.getProxy().getPluginManager().getPlugin("luckperms").isPresent()) {
            try {
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
            } catch (Exception ignored) {
            }
        }
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
                                    sendMessageToPlayer(player, message);
                                }
                            } else {
                                sendMessageToPlayer(player, message);
                            }
                        } else {
                            sendMessageToPlayer(player, message);
                        }
                    }
                } else {
                    if (plugin.getConfig().getBoolean("permission.permissions")) {
                        if (plugin.getConfig().getBoolean("permission.hide_message")) {
                            if (player.hasPermission("ppn.view")) {
                                sendMessageToPlayer(player, message);
                            }
                        } else {
                            sendMessageToPlayer(player, message);
                        }
                    } else {
                        sendMessageToPlayer(player, message);
                    }
                }
            }
        }
    }

    private static void sendMessageToPlayer(Player player, String message) {
        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .build();
        String[] lines = message.split("\n");
        for (String line : lines) {
            player.sendMessage(serializer.deserialize(line));
        }
    }
}