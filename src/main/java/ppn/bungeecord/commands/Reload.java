package ppn.bungeecord.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ppn.bungeecord.BungeePlayerNotify;

import java.util.HashSet;

public class Reload extends Command {

    private final BungeePlayerNotify plugin;

    public Reload(BungeePlayerNotify plugin) {
        super("reloadProxyNotifyConfig");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            if (args.length < 1) {
                if (sender.hasPermission("ppn.reloadProxyNotifyConfig")) {
                    sender.sendMessage("Reload done");
                    plugin.saveDefaultConfig();
                    plugin.loadConfig();

                    plugin.getServerNames().clear();
                    plugin.getConfig().getKeys("ServerNames").forEach(server -> plugin.getServerNames().put(server.toLowerCase(), plugin.getConfig().getString("ServerNames." + server)));

                    plugin.setDisabledServers(new HashSet<>(plugin.getConfig().getStringList("DisabledServers")));
                    plugin.setPrivateServers(new HashSet<>(plugin.getConfig().getStringList("PrivateServers")));
                    plugin.setLimboServers(new HashSet<>(plugin.getConfig().getStringList("LimboServers")));
                    plugin.setNoVanishNotifications(plugin.getConfig().getBoolean("disable_vanish_notifications"));
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have ppn.reloadProxyNotifyConfig permission to use this command");
                }
            }
        } else {
            plugin.getProxy().broadcast("Reload done");
            plugin.saveDefaultConfig();
            plugin.loadConfig();

            plugin.getServerNames().clear();
            plugin.getConfig().getKeys("ServerNames").forEach(server -> plugin.getServerNames().put(server.toLowerCase(), plugin.getConfig().getString("ServerNames." + server)));

            plugin.setDisabledServers(new HashSet<>(plugin.getConfig().getStringList("DisabledServers")));
            plugin.setPrivateServers(new HashSet<>(plugin.getConfig().getStringList("PrivateServers")));
            plugin.setLimboServers(new HashSet<>(plugin.getConfig().getStringList("LimboServers")));
            plugin.setNoVanishNotifications(plugin.getConfig().getBoolean("disable_vanish_notifications"));
        }
    }
}