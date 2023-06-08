package bpn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        // Create the plugin data folder if it does not already exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultConfig();
        loadConfig();

        // Register the plugin's event listener
        getProxy().getPluginManager().registerListener(this, this);
        if (config.getString("join_message").contains("%lp_prefix%")){
            luckPerms = LuckPermsProvider.get();
        }

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
        sendMessage("join_message", event.getPlayer(), null);
    }

    // Called when a player switches servers in the network
    @EventHandler
    public void onSwitch(ServerConnectedEvent event) {
        sendMessage("switch_message", event.getPlayer(), event.getServer().getInfo().getName());
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        sendMessage("leave_message", event.getPlayer(), null);
    }

    public void sendMessage(String type, ProxiedPlayer targetPlayer, String server) {
        if (config.getBoolean("permission.permissions")) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("bpn.notify")) {
                    String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                    if (finalMessage.equals("")) {
                        return;
                    }
                    if (type.equals("switch_message")) {
                        finalMessage = finalMessage.replace("%server%", server);
                    }
                    finalMessage = finalMessage.replace("&", "§");
                    finalMessage = finalMessage.replace("%lp_prefix%", luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer).getCachedData().getMetaData().getPrefix());
                    for (ProxiedPlayer pl : getProxy().getPlayers()) {
                        if (config.getBoolean("permissions.hide_message")) {
                            if (pl.hasPermission("bpn.view")) {
                                pl.sendMessage(finalMessage);
                            }
                        } else {
                            getProxy().broadcast(finalMessage);
                        }
                    }
                } else {
                    String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                    if (finalMessage.equals("")) {
                        return;
                    }
                    if (type.equals("switch_message")) {
                        finalMessage = finalMessage.replace("%server%", server);
                    }
                    finalMessage = finalMessage.replace("&", "§");
                    finalMessage = finalMessage.replace("%lp_prefix%", luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer).getCachedData().getMetaData().getPrefix());
                    for (ProxiedPlayer pl : getProxy().getPlayers()) {
                        if (config.getBoolean("permissions.hide_message")) {
                            if (pl.hasPermission("bpn.view")) {
                                pl.sendMessage(finalMessage);
                            }
                        } else {
                            getProxy().broadcast(finalMessage);
                        }
                    }
                }
            } else {
                String finalMessage = config.getString(type).replace("%player%", targetPlayer.getName());
                if (finalMessage.equals("")) {
                    return;
                }
                if (type.equals("switch_message")) {
                    finalMessage = finalMessage.replace("%server%", server);
                }
                finalMessage = finalMessage.replace("&", "§");
                finalMessage = finalMessage.replace("%lp_prefix%", luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(targetPlayer).getCachedData().getMetaData().getPrefix());
                getProxy().broadcast(finalMessage);
            }
        }
    }

}
