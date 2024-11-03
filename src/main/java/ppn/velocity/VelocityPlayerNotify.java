package ppn.velocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import ppn.ConfigManager;
import ppn.velocity.commands.Reload;
import ppn.velocity.commands.ToggleMessages;
import ppn.velocity.utils.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "proxyplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP", version = "2.3", url = "https://www.spigotmc.org/resources/bungeeplayernotify.108035/", dependencies = {
        @Dependency(id = "luckperms", optional = true)
})
public class VelocityPlayerNotify {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private final Set<UUID> messageToggles = new HashSet<>();
    private Set<String> disabledServers;
    private Set<String> privateServers;
    private Set<String> limboServers;
    private Set<String> disabledPlayers;
    private boolean noVanishNotifications;
    private final Map<UUID, String> playerLastServer = new HashMap<>();
    private final Map<String, String> serverNames = new HashMap<>();
    private ConfigManager config;

    @Inject
    public VelocityPlayerNotify(ProxyServer proxy, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        if (Files.exists(Paths.get(dataDirectory + "/config.toml"))){
            proxy.getConsoleCommandSource().sendMessage(Component.text("Old config file has been detected! Move over all settings to new config.yml instead of the config.toml to make the plugin work!!!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
            Path filePath = Paths.get(dataDirectory + "/IMPORTANTpleaseREAD.txt");
            String message = "Please move all settings to new config.yml instead of the config.toml to make the plugin work!";

            try {
                List<String> updatedLines = Files.lines(filePath)
                        .map(line -> line + " " + message)
                        .collect(Collectors.toList());

                Files.write(filePath, updatedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        saveDefaultConfig();
        loadConfig();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        saveDefaultConfig();
        loadConfig();
        this.metricsFactory.make(this, 18744);
        this.proxy.getCommandManager().register("reloadProxyNotifyConfig", new Reload(this));
        this.proxy.getCommandManager().register("togglemessages", new ToggleMessages(this));
        this.proxy.getEventManager().register(this, new EventListener(this));
        disabledServers = new HashSet<>(config.getStringList("DisabledServers"));
        privateServers = new HashSet<>(config.getStringList("PrivateServers"));
        limboServers = new HashSet<>(config.getStringList("LimboServers"));
        disabledPlayers = new HashSet<>(config.getStringList("DisabledPlayers"));
        this.noVanishNotifications = config.getBoolean("disable_vanish_notifications");
        config.addDefault("join_last_server", false);
        config.saveConfig();
        config.getKeys("ServerNames").forEach(server -> serverNames.put(server.toLowerCase(), config.getString("ServerNames." + server)));
    }


    public void saveDefaultConfig() {
        File file = new File(dataDirectory.toFile(), "config.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig() {
        File file = new File(dataDirectory.toFile(), "config.yml");
        config = new ConfigManager(dataDirectory.toFile(), "config.yml");
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ConfigManager getConfig() {
        return config;
    }

    public Set<UUID> getMessageToggles() {
        return messageToggles;
    }

    public Set<String> getDisabledServers() {
        return disabledServers;
    }

    public Set<String> getPrivateServers() {
        return privateServers;
    }

    public Set<String> getLimboServers() {
        return limboServers;
    }

    public Set<String> getDisabledPlayers() {
        return disabledPlayers;
    }

    public boolean isNoVanishNotifications() {
        return noVanishNotifications;
    }

    public Map<UUID, String> getPlayerLastServer() {
        return playerLastServer;
    }

    public Map<String, String> getServerNames() {
        return serverNames;
    }

    public void setDisabledServers(Set<String> disabledServers) {
        this.disabledServers = disabledServers;
    }

    public void setPrivateServers(Set<String> privateServers) {
        this.privateServers = privateServers;
    }

    public void setLimboServers(Set<String> limboServers) {
        this.limboServers = limboServers;
    }

    public void setNoVanishNotifications(boolean noVanishNotifications) {
        this.noVanishNotifications = noVanishNotifications;
    }
}