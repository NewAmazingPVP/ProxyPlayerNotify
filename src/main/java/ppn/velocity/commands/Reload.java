package ppn.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import ppn.velocity.ConfigLoader;
import ppn.velocity.VelocityPlayerNotify;

import java.util.HashSet;

public class Reload implements SimpleCommand {

    private final VelocityPlayerNotify plugin;

    public Reload(VelocityPlayerNotify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        plugin.setConfig(ConfigLoader.loadConfig(plugin.getDataDirectory()));
        plugin.setDisabledServers(new HashSet<>(plugin.getConfig().getList("disabled_servers")));
        plugin.setPrivateServers(new HashSet<>(plugin.getConfig().getList("private_servers")));
        ConfigLoader.loadServerNames(plugin.getConfig(), plugin.getServerNames());
        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("Reload done"));
    }
}