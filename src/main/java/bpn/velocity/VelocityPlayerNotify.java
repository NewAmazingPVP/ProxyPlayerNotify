package bpn.velocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
//import net.luckperms.api.LuckPerms;
//import net.luckperms.api.LuckPermsProvider;
//import net.luckperms.api.model.user.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "bungeeplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP")
public class VelocityPlayerNotify {

    private Toml config;
    //private LuckPerms luckPerms;
    private final ProxyServer proxy;
    private final Path dataDirectory;

    @Inject
    public VelocityPlayerNotify(ProxyServer proxy, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        config = loadConfig(dataDirectory);
        //if (config.getString("join_message").contains("%lp_prefix%") || config.getString("switch_message").contains("%lp_prefix%") || config.getString("leave_message").contains("%lp_prefix%")){
            //luckPerms = LuckPermsProvider.get();
        //}
    }

    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        return new Toml().read(file);
    }

    @Subscribe
    public void onJoin(PlayerChooseInitialServerEvent event) {
        sendMessage("join_message", event.getPlayer(), null, null);
    }

    @Subscribe
    public void onSwitch(ServerConnectedEvent event) {
        sendMessage("switch_message", event.getPlayer(), event.getServer().getServerInfo().getName(), event.getPreviousServer().toString());
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {

        sendMessage("leave_message", event.getPlayer(), null, null);
    }

    public void sendMessage(String type, Player targetPlayer, String connectedServer, String disconnectedServer) {
        if (config.getBoolean(("permission.permissions"))) {
            if (config.getBoolean("permission.notify_message")) {
                if (targetPlayer.hasPermission("bpn.notify")) {
                    String finalMessage = config.getString(type).replace("%player%", targetPlayer.getUsername());
                    if (finalMessage.equals("")) {
                        return;
                    }
                    if (type.equals("switch_message")) {
                        finalMessage = finalMessage.replace("%connectedServer%", connectedServer);
                        finalMessage = finalMessage.replace("%previousServer%", disconnectedServer);
                    }
                    finalMessage = finalMessage.replace("&", "§");
                    /*if (finalMessage.contains("%lp_prefix%")) {
                        luckPerms = LuckPermsProvider.get();
                        User user = luckPerms.getUserManager().getUser(targetPlayer.getUniqueId());
                        String prefix = user.getCachedData().getMetaData().getPrefix();
                        if (!(prefix == null)) {
                            prefix = prefix.replace("&", "§");
                            finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                        }
                    }*/
                    Component translatedComponent = Component.text(finalMessage);
                    if (config.getBoolean("permission.hide_message")) {
                        for (Player pl : proxy.getAllPlayers()) {
                            if (pl.hasPermission("bpn.view")) {
                                pl.sendMessage(translatedComponent);
                            }
                        }
                    } else {
                        proxy.getAllPlayers().forEach(player -> player.sendMessage(translatedComponent));
                    }
                }
            } else if (config.getBoolean("permission.hide_message")) {
                String finalMessage = config.getString(type).replace("%player%", targetPlayer.getUsername());
                if (finalMessage.equals("")) {
                    return;
                }
                if (type.equals("switch_message")) {
                    finalMessage = finalMessage.replace("%server%", connectedServer);
                    finalMessage = finalMessage.replace("%previousServer%", disconnectedServer);
                }
                finalMessage = finalMessage.replace("&", "§");
                /*if (finalMessage.contains("%lp_prefix%")) {
                    luckPerms = LuckPermsProvider.get();
                    User user = luckPerms.getUserManager().getUser(targetPlayer.getUniqueId());
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (!(prefix == null)) {
                        prefix = prefix.replace("&", "§");
                        finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                    }
                }*/
                Component translatedComponent = Component.text(finalMessage);
                for (Player pl : proxy.getAllPlayers()) {
                    if (pl.hasPermission("bpn.view")) {
                        pl.sendMessage(translatedComponent);

                    }
                }
            }
        } else {
            String finalMessage = config.getString(type).replace("%player%", targetPlayer.getUsername());
            if (finalMessage.equals("")) {
                return;
            }
            if (type.equals("switch_message")) {
                finalMessage = finalMessage.replace("%server%", connectedServer);
                finalMessage = finalMessage.replace("%previousServer%", disconnectedServer);
            }
            finalMessage = finalMessage.replace("&", "§");
            /*if (finalMessage.contains("%lp_prefix%")) {
                luckPerms = LuckPermsProvider.get();
                User user = luckPerms.getUserManager().getUser(targetPlayer.getUniqueId());
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (!(prefix == null)) {
                    prefix = prefix.replace("&", "§");
                    finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                }
            }*/
            Component translatedComponent = Component.text(finalMessage);
            for (Player pl : proxy.getAllPlayers()) {
                pl.sendMessage(translatedComponent);
            }
        }
    }
}
