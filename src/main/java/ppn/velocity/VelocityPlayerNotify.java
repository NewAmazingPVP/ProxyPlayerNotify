package ppn.velocity;

import ppn.velocity.commands.Reload;
import ppn.velocity.commands.ToggleMessages;
import ppn.velocity.utils.Metrics;
import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(id = "proxyplayernotify", name = "ProxyPlayerNotify", authors = "NewAmazingPVP", version = "2.2.2", url = "https://www.spigotmc.org/resources/bungeeplayernotify.108035/", dependencies = {
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
    private boolean noVanishNotifications;
    private final ConcurrentHashMap<UUID, String> playerLastServer = new ConcurrentHashMap<>();
    private final Map<String, String> serverNames = new HashMap<>();
    private Toml config;

    @Inject
    public VelocityPlayerNotify(ProxyServer proxy, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        this.config = ConfigLoader.loadConfig(dataDirectory);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = ConfigLoader.loadConfig(dataDirectory);
        this.metricsFactory.make(this, 18744);
        this.proxy.getCommandManager().register("reloadProxyNotifyConfig", new Reload(this));
        this.proxy.getCommandManager().register("togglemessages", new ToggleMessages(this));
        this.disabledServers = new HashSet<>(config.getList("disabled_servers"));
        this.privateServers = new HashSet<>(config.getList("private_servers"));
        this.limboServers = new HashSet<>(config.getList("limbo_servers"));
        this.noVanishNotifications = config.getBoolean("disable_vanish_notifications");
        ConfigLoader.loadServerNames(config, serverNames);
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Toml getConfig() {
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

    public boolean isNoVanishNotifications() {
        return noVanishNotifications;
    }

    public ConcurrentHashMap<UUID, String> getPlayerLastServer() {
        return playerLastServer;
    }

    public Map<String, String> getServerNames() {
        return serverNames;
    }

    public void setConfig(Toml config) {
        this.config = config;
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