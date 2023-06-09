package bpn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "bungeeplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP")
public class VelocityPlayerNotify {

    private ConfigurationNode config;
    private LuckPerms luckPerms;
    private final ProxyServer proxy;
    private final Path dataDirectory;

    @Inject
    public VelocityPlayerNotify(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.proxy = server;
        this.dataDirectory = dataDirectory;
    }

    public void saveDefaultConfig() throws IOException {
        Path configFile = dataDirectory.resolve("config.conf");
        if (!Files.exists(configFile)) {
            Files.createDirectories(configFile.getParent());
            Files.copy(getClass().getResourceAsStream("/config.conf"), configFile);
        }
    }

    public void loadConfig() throws IOException {
        Path configFile = dataDirectory.resolve("config.conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(configFile).build();
        config = loader.load();
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
        if (config.getNode("permission", "permissions").getBoolean()) {
            if (config.getNode("permission", "notify_message").getBoolean()) {
                if (targetPlayer.hasPermission("bpn.notify")) {
                    String finalMessage = config.getNode(type).getString().replace("%player%", targetPlayer.getUsername());
                    if (finalMessage.equals("")) {
                        return;
                    }
                    if (type.equals("switch_message")) {
                        finalMessage = finalMessage.replace("%server%", connectedServer);
                    }
                    finalMessage = finalMessage.replace("&", "§");
                    if (finalMessage.contains("%lp_prefix%")) {
                        luckPerms = LuckPermsProvider.get();
                        User user = luckPerms.getUserManager().getUser(targetPlayer.getUniqueId());
                        String prefix = user.getCachedData().getMetaData().getPrefix();
                        if (!(prefix == null)) {
                            prefix = prefix.replace("&", "§");
                            finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                        }
                    }
                    if (config.getNode("permission", "hide_message").getBoolean()) {
                        for (Player pl : proxy.getAllPlayers()) {
                            if (pl.hasPermission("bpn.view")) {
                                pl.sendMessage(finalMessage);
                            }
                        }
                    } else {
                        proxy.getAllPlayers().forEach(player -> player.sendMessage(finalMessage))
                    }
                }
            } else if (config.getNode("permission", "hide_message").getBoolean()) {
                String finalMessage = config.getNode(type).getString().replace("%player%", targetPlayer.getUsername());
                if (finalMessage.equals("")) {
                    return;
                }
                if (type.equals("switch_message")) {
                    finalMessage = finalMessage.replace("%server%", connectedServer);
                }
                if (finalMessage.contains("%lp_prefix%")){
                    luckPerms = LuckPermsProvider.get();
                    User user = luckPerms.getUserManager().getUser(targetPlayer.getUniqueId());
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (!(prefix == null)){
                        prefix = prefix.replace("&", "§");
                        finalMessage = finalMessage.replace("%lp_prefix%", prefix);
                    }
                }
                finalMessage = finalMessage.replace("&", "§");
                for (Player pl : proxy.getAllPlayers()) {
                    if (pl.hasPermission("bpn.view")) {
                        pl.sendMessage(finalMessage);
                    }
                }
            }
        }
        else {
