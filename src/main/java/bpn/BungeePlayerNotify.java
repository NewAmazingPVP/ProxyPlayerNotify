package bpn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;

    // Called when the plugin is enabled
    @Override
    public void onEnable() {
        // Create the plugin data folder if it does not already exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create the plugin data folder if it does not exist
        saveDefaultConfig();
        loadConfig();

        // Register the plugin's event listener
        getProxy().getPluginManager().registerListener(this, this);
    }

    public void saveDefaultConfig(){
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()){
            file.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }

    public void loadConfig(){
        File file = new File(getDataFolder(), "config.yml");
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Called when a player joins the network
    @EventHandler
    public void onJoin(PostLoginEvent event) {
        // Get the join message from the config file and replace placeholders with actual values
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String message = config.getString("join_message").replace("%player%", event.getPlayer().getName());
                String finalMessage = message.replace("&", "§");
                if (finalMessage.equals("")) {
                    return;
                }
                getProxy().broadcast(finalMessage);
            }
        } else {
            String message = config.getString("join_message").replace("%player%", event.getPlayer().getName());
            String finalMessage = message.replace("&", "§");
            if (finalMessage.equals("")) {
                return;
            }
            getProxy().broadcast(finalMessage);
        }
    }

    // Called when a player switches servers in the network
    @EventHandler
    public void onSwitch(ServerConnectedEvent event) {
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String message = config.getString("switch_message").replace("%player%", event.getPlayer().getName());
                message = message.replace("%server%", event.getServer().getInfo().getName());
                String finalMessage = message.replace("&", "§");
                if (finalMessage.equals("")) {
                    return;
                }
                getProxy().broadcast(finalMessage);
            }
        } else {
            String message = config.getString("switch_message").replace("%player%", event.getPlayer().getName());
            message = message.replace("%server%", event.getServer().getInfo().getName());
            String finalMessage = message.replace("&", "§");
            if (finalMessage.equals("")) {
                return;
            }
            getProxy().broadcast(finalMessage);
        }


    }

    // Called when a player leaves the network
    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String message = config.getString("leave_message").replace("%player%", event.getPlayer().getName());
                String finalMessage = message.replace("&", "§");
                if (finalMessage.equals("")) {
                    return;
                }
                getProxy().broadcast(finalMessage);
            }
        } else {
            String message = config.getString("leave_message").replace("%player%", event.getPlayer().getName());
            String finalMessage = message.replace("&", "§");
            if (finalMessage.equals("")) {
                return;
            }
            getProxy().broadcast(finalMessage);
        }
    }

}
