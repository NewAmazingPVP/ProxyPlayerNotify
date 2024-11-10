package ppn.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import ppn.velocity.VelocityPlayerNotify;

import java.util.HashSet;

public class Reload implements SimpleCommand {

    private final VelocityPlayerNotify plugin;

    public Reload(VelocityPlayerNotify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        plugin.saveDefaultConfig();
        plugin.loadConfig();
        plugin.setDisabledServers(new HashSet<>(plugin.getConfig().getStringList("DisabledServers")));
        plugin.setPrivateServers(new HashSet<>(plugin.getConfig().getStringList("PrivateServers")));
        plugin.setLimboServers(new HashSet<>(plugin.getConfig().getStringList("LimboServers")));
        plugin.setNoVanishNotifications(plugin.getConfig().getBoolean("disable_vanish_notifications"));
        plugin.setDisabledPlayers(new HashSet<>(plugin.getConfig().getStringList("DisabledPlayers")));
        plugin.getServerNames().clear();
        plugin.getConfig().getKeys("ServerNames").forEach(server -> plugin.getServerNames().put(server.toLowerCase(), plugin.getConfig().getString("ServerNames." + server)));
        plugin.getConfig().saveConfig();
        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("Reload done"));
    }
}