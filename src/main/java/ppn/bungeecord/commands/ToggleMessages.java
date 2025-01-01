package ppn.bungeecord.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import ppn.bungeecord.BungeePlayerNotify;

import java.util.HashSet;
import java.util.UUID;

public class ToggleMessages extends Command {

    private final BungeePlayerNotify plugin;
    public static final HashSet<UUID> playerToggle = new HashSet<>();

    public ToggleMessages(BungeePlayerNotify plugin) {
        super("togglemessages");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            if (playerToggle.contains(player.getUniqueId())) {
                playerToggle.remove(player.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Message notifications toggled on");
            } else {
                playerToggle.add(player.getUniqueId());
                sender.sendMessage(ChatColor.RED + "Message notifications toggled off");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        }
    }
}