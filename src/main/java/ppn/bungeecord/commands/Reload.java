package ppn.bungeecord.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ppn.bungeecord.BungeePlayerNotify;

public class Reload extends Command {

    private final BungeePlayerNotify plugin;

    public Reload(BungeePlayerNotify plugin) {
        super("reloadProxyNotifyConfig", null, "ppnreload", "ppnr", "ppnrl", "ppn-reload");
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
                    plugin.refreshRuntimeConfig();
                    plugin.getConfig().saveConfig();
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have ppn.reloadProxyNotifyConfig permission to use this command");
                }
            }
        } else {
            sender.sendMessage("Reload done");
            plugin.saveDefaultConfig();
            plugin.loadConfig();
            plugin.refreshRuntimeConfig();
            plugin.getConfig().saveConfig();
        }
    }
}
