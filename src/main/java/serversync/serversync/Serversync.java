package serversync.serversync;

import java.io.File;
import java.io.IOException;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class Serversync extends Plugin implements Listener {

    // Called when the plugin is enabled    
    @Override
    public void onEnable() {
        // Create the plugin data folder if it does not already exist        
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create the plugin data folder if it does not exist        
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();

                // Set default configuration values                
                Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("join_message", "%player% has joined the network!");
                configuration.set("switch_message", "%player% has switched/joined to the %server% server");
                configuration.set("leave_message", "%player% has left the network");
                configuration.set("color", "b");

                // Save the configuration file                
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Register the plugin's event listener        
        getProxy().getPluginManager().registerListener(this, this);
    }

    //Called when a player joins the network
    @EventHandler
    public void onJoin(PostLoginEvent event) {
        // Get the join message from the config file and replace placeholders with actual values        
        String message = getConfig().getString("join_message");
        message = message.replace("%player%", event.getPlayer().getName());

        // Broadcast the message to all servers in the network with the color chosen in the config file
        getProxy().broadcast("§" + getConfig().getString("color") + message);
    }
    
    // Called when a player switches servers in the network    
    @EventHandler
    public void onSwitch(ServerConnectedEvent event) {
        String message = getConfig().getString("switch_message");
        message = message.replace("%player%", event.getPlayer().getName());
        message = message.replace("%server%", event.getServer().getInfo().getName());
        getProxy().broadcast("§" + getConfig().getString("color") + message);
    }

    // Called when a player leaves the network    
    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String message = getConfig().getString("leave_message");
        message = message.replace("%player%", event.getPlayer().getName());
        getProxy().broadcast("§" + getConfig().getString("color") + message);
    }

    private Configuration getConfig() {
    // Attempt to load the config file as a YamlConfiguration object
    try {
        File configFile = new File(getDataFolder(), "config.yml");
        Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        return configuration;
    } catch (IOException e) {
        // Print the stack trace if there is an error loading the file
        e.printStackTrace();
    }
    // Return null if the configuration couldn't be loaded
    return null;
}

}
