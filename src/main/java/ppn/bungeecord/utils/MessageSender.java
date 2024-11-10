package ppn.bungeecord.utils;

import de.myzelyam.api.vanish.BungeeVanishAPI;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ppn.bungeecord.BungeePlayerNotify;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static ppn.bungeecord.commands.ToggleMessages.playerToggle;

public class MessageSender {

    private static final Pattern HEX_REGEX = Pattern.compile("&#([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])", Pattern.CASE_INSENSITIVE);

    public static void sendMessage(BungeePlayerNotify plugin, String type, ProxiedPlayer targetPlayer, String server, String lastServer) {
        plugin.saveDefaultConfig();
        plugin.loadConfig();

        if (server != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(server.toLowerCase())) {
            return;
        }

        if (plugin.getDisabledPlayers().contains(targetPlayer.getName().toLowerCase())) {
            return;
        }

        if (lastServer != null && plugin.getPrivateServers() != null && plugin.getPrivateServers().contains(lastServer.toLowerCase())) {
            return;
        }

        if (plugin.isNoVanishNotifications() && BungeeVanishAPI.isInvisible(targetPlayer)) {
            return;
        }

        if (plugin.getConfig().getBoolean("permission.permissions")) {
            if (plugin.getConfig().getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("ppn.notify")) {
                    sendFormattedMessage(plugin, type, targetPlayer, server, lastServer);
                }
            } else {
                sendFormattedMessage(plugin, type, targetPlayer, server, lastServer);
            }
        } else {
            sendFormattedMessage(plugin, type, targetPlayer, server, lastServer);
        }
    }

    public static void sendPrivateMessage(BungeePlayerNotify plugin, String type, ProxiedPlayer targetPlayer, String server) {
        plugin.saveDefaultConfig();
        plugin.loadConfig();

        String finalMessage = plugin.getConfig().getString(type).replace("%player%", targetPlayer.getName());
        if (finalMessage.isEmpty()) {
            return;
        }
        if (server != null) {
            finalMessage = finalMessage.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
        }
        if (plugin.getDisabledPlayers().contains(targetPlayer.getName().toLowerCase())) {
            return;
        }
        if (finalMessage.contains("%lp_prefix%")) {
            User user = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
        }
        if (finalMessage.contains("%lp_suffix%")) {
            User user = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String suffix = user.getCachedData().getMetaData().getSuffix();
            if (suffix != null) {
                finalMessage = finalMessage.replace("%lp_suffix%", suffix);
            }
        }
        finalMessage = finalMessage.replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        sendMessageToPlayer(targetPlayer, finalMessage);
    }

    private static void sendFormattedMessage(BungeePlayerNotify plugin, String type, ProxiedPlayer targetPlayer, String server, String lastServer) {
        String finalMessage = plugin.getConfig().getString(type).replace("%player%", targetPlayer.getName());
        if (finalMessage.isEmpty()) {
            return;
        }

        if (type.equals("switch_message") || type.equals("join_message") || type.equals("leave_message")) {
            if (server != null) {
                finalMessage = finalMessage.replace("%server%", plugin.getServerNames().getOrDefault(server.toLowerCase(), server));
            }
            if (lastServer != null) {
                //same here
                finalMessage = finalMessage.replace("%last_server%", plugin.getServerNames().getOrDefault(lastServer.toLowerCase(), lastServer));
            }
        }
        if (finalMessage.contains("%lp_prefix%")) {
            User user = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
        }
        if (finalMessage.contains("%lp_suffix%")) {
            User user = plugin.getLuckPerms().getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer);
            String suffix = user.getCachedData().getMetaData().getSuffix();
            if (suffix != null) {
                finalMessage = finalMessage.replace("%lp_suffix%", suffix);
            }
        }
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        finalMessage = finalMessage.replace("%time%", time);

        for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
            if (!playerToggle.contains(pl.getUniqueId())) {
                if (pl.getServer() != null && plugin.getDisabledServers() != null) {
                    if (!plugin.getDisabledServers().contains(pl.getServer().getInfo().getName().toLowerCase())) {
                        if (plugin.getConfig().getBoolean("permission.permissions")) {
                            if (plugin.getConfig().getBoolean("permission.hide_message")) {
                                if (pl.hasPermission("ppn.view")) {
                                    sendMessageToPlayer(pl, finalMessage);
                                }
                            } else {
                                sendMessageToPlayer(pl, finalMessage);
                            }
                        } else {
                            sendMessageToPlayer(pl, finalMessage);
                        }
                    }
                } else {
                    if (plugin.getConfig().getBoolean("permission.permissions")) {
                        if (plugin.getConfig().getBoolean("permission.hide_message")) {
                            if (pl.hasPermission("ppn.view")) {
                                sendMessageToPlayer(pl, finalMessage);
                            }
                        } else {
                            sendMessageToPlayer(pl, finalMessage);
                        }
                    } else {
                        sendMessageToPlayer(pl, finalMessage);
                    }
                }
            }
        }
    }

    private static void sendMessageToPlayer(ProxiedPlayer player, String message) {
        message = replace(message);
        message = message.replace("&", "§");
        message = ChatColor.translateAlternateColorCodes('§', message);
        String[] lines = message.split("\n");
        for (String line : lines) {
            player.sendMessage(line);
        }
    }

    private static String replace(String s) {
        return HEX_REGEX.matcher(s).replaceAll("&x&$1&$2&$3&$4&$5&$6");
    }
}