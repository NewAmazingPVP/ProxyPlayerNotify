package bpn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Supplier;

import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BungeePlayerNotify extends Plugin implements Listener {

    private Configuration config;
    private LuckPerms luckPerms;



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
        // Get the join message from the config file and replace placeholders with actual values
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String finalMessage = config.getString("join_message").replace("%player%", event.getPlayer().getName());
                if (finalMessage.equals("")) {
                    return;
                }
                if (finalMessage.contains("%lp_prefix%")){
                    ProxiedPlayer player = event.getPlayer();
                    User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                }
                finalMessage = finalMessage.replace("&", "§");
                UUID dumb = event.getPlayer().getUniqueId();
                Player p = Bukkit.getPlayer(UUID.fromString(String.valueOf(dumb)));
                finalMessage = PlaceholderAPI. setPlaceholders(p , finalMessage);
                getProxy().broadcast(finalMessage);
            }
        } else {
            String finalMessage = config.getString("join_message").replace("%player%", event.getPlayer().getName());
            if (finalMessage.equals("")) {
                return;
            }
            if (finalMessage.contains("%lp_prefix%")){
                ProxiedPlayer player = event.getPlayer();
                User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                String prefix = user.getCachedData().getMetaData().getPrefix();
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
            finalMessage = finalMessage.replace("&", "§");
            getProxy().broadcast(finalMessage);
        }
    }

    // Called when a player switches servers in the network
    @EventHandler
    public void onSwitch(ServerConnectedEvent event) {
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String finalMessage = config.getString("switch_message").replace("%player%", event.getPlayer().getName());
                finalMessage = finalMessage.replace("%server%", event.getServer().getInfo().getName());
                if (finalMessage.equals("")) {
                    return;
                }
                if (finalMessage.contains("%lp_prefix%")){
                    ProxiedPlayer player = event.getPlayer();
                    User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                }
                finalMessage = finalMessage.replace("&", "§");
                getProxy().broadcast(finalMessage);
            }
        } else {
            String finalMessage = config.getString("switch_message").replace("%player%", event.getPlayer().getName());
            finalMessage = finalMessage.replace("%server%", event.getServer().getInfo().getName());
            if (finalMessage.equals("")) {
                return;
            }
            if (finalMessage.contains("%lp_prefix%")){
                ProxiedPlayer player = event.getPlayer();
                User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                String prefix = user.getCachedData().getMetaData().getPrefix();
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
            finalMessage = finalMessage.replace("&", "§");
            getProxy().broadcast(finalMessage);
        }


    }

    // Called when a player leaves the network
    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (config.getBoolean("permissions")) {
            if (event.getPlayer().hasPermission("bpn.notify")) {
                String finalMessage = config.getString("leave_message").replace("%player%", event.getPlayer().getName());
                if (finalMessage.equals("")) {
                    return;
                }
                if (finalMessage.contains("%lp_prefix%")){
                    ProxiedPlayer player = event.getPlayer();
                    User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                }
                finalMessage = finalMessage.replace("&", "§");
                getProxy().broadcast(finalMessage);
            }
        } else {
            String finalMessage = config.getString("leave_message").replace("%player%", event.getPlayer().getName());
            if (finalMessage.equals("")) {
                return;
            }
            if (finalMessage.contains("%lp_prefix%")){
                ProxiedPlayer player = event.getPlayer();
                User user = luckPerms.getPlayerAdapter(ProxiedPlayer.class).getUser(player);
                String prefix = user.getCachedData().getMetaData().getPrefix();
                finalMessage = finalMessage.replace("%lp_prefix%", prefix);
            }
            finalMessage = finalMessage.replace("&", "§");
            getProxy().broadcast(finalMessage);
        }
    }

}
