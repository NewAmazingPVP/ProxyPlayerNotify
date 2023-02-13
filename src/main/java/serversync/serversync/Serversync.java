package serversync.serversync;

import java.io.File;
import java.io.IOException;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class Serversync extends Plugin implements Listener {

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("join_message", "%player% has joined the network!");
                configuration.set("switch_message", "%player% has switched to server %server%");
                configuration.set("leave_message", "%player% has left the network");
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        String message = getConfig().getString("join_message");
        message = message.replace("%player%", event.getPlayer().getName());
        getProxy().broadcast(message);
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        String message = getConfig().getString("switch_message");
        message = message.replace("%player%", event.getPlayer().getName());
        message = message.replace("%server%", event.getFrom().getName());
        getProxy().broadcast(message);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String message = getConfig().getString("leave_message");
        message = message.replace("%player%", event.getPlayer().getName());
        getProxy().broadcast(message);
    }

    private Configuration getConfig() {
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
